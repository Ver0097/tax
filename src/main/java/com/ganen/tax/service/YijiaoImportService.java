package com.ganen.tax.service;

import com.ganen.tax.dto.ImportProgress;
import com.ganen.tax.entity.YijiaoInfo;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class YijiaoImportService {

    private static final Logger logger = LoggerFactory.getLogger(YijiaoImportService.class);

    private static final int INSERT_BATCH_SIZE = 1000;
    private static final int START_ROW_INDEX = 8;
    private static final int COL_ROW_FLAG = 0;
    private static final int COL_USER_NAME = 1;
    private static final int COL_ID_CARD = 3;
    private static final int COL_PAID_AMOUNT = 40;

    @Autowired
    private YijiaoInfoService yijiaoInfoService;

    @Autowired
    private ApplicationContext applicationContext;

    private final Map<String, ImportProgress> progressMap = new ConcurrentHashMap<>();

    public String startImport(MultipartFile file) {
        String taskId = UUID.randomUUID().toString().replace("-", "");
        ImportProgress progress = ImportProgress.create(taskId);
        progressMap.put(taskId, progress);

        try {
            String originalFilename = file.getOriginalFilename();
            String suffix = getFileSuffix(originalFilename);
            Path tempFile = Files.createTempFile("yijiao_import_", suffix);
            file.transferTo(tempFile.toFile());
            applicationContext.getBean(YijiaoImportService.class).asyncImport(tempFile.toString(), taskId);
        } catch (Exception e) {
            progressMap.remove(taskId);
            throw new RuntimeException(e.getMessage(), e);
        }

        return taskId;
    }

    @Async
    public void asyncImport(String filePath, String taskId) {
        ImportProgress progress = progressMap.get(taskId);
        int successCount = 0;
        int failCount = 0;
        int processedCount = 0;

        File excelFile = new File(filePath);
        List<YijiaoInfo> buffer = new ArrayList<>(INSERT_BATCH_SIZE);
        DataFormatter formatter = new DataFormatter();

        IOUtils.setByteArrayMaxOverride(Integer.MAX_VALUE);

        try (InputStream is = Files.newInputStream(excelFile.toPath()); Workbook workbook = WorkbookFactory.create(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                progress.setTotalRows(0);
                progress.complete(0, 0);
                progressMap.put(taskId, progress);
                return;
            }

            int lastRowNum = sheet.getLastRowNum();
            for (int i = START_ROW_INDEX; i <= lastRowNum; i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }

                String rowFlag = trim(formatter.formatCellValue(row.getCell(COL_ROW_FLAG)));
                if (!isPureNumber(rowFlag)) {
                    break;
                }

                processedCount++;

                BigDecimal paidAmount = parseBigDecimal(formatter.formatCellValue(row.getCell(COL_PAID_AMOUNT)));
                if (paidAmount.compareTo(BigDecimal.ZERO) == 0) {
                    failCount++;
                    progress.setTotalRows(processedCount);
                    progress.update(processedCount, successCount, failCount);
                    progressMap.put(taskId, progress);
                    continue;
                }

                YijiaoInfo info = new YijiaoInfo();
                info.setPaidAmount(paidAmount);
                info.setUserName(trim(formatter.formatCellValue(row.getCell(COL_USER_NAME))));
                info.setIdCard(trim(formatter.formatCellValue(row.getCell(COL_ID_CARD))));
                buffer.add(info);

                if (buffer.size() >= INSERT_BATCH_SIZE) {
                    yijiaoInfoService.saveBatch(buffer, INSERT_BATCH_SIZE);
                    successCount += buffer.size();
                    buffer.clear();
                }

                progress.setTotalRows(processedCount);
                progress.update(processedCount, successCount, failCount);
                progressMap.put(taskId, progress);
            }

            if (!buffer.isEmpty()) {
                yijiaoInfoService.saveBatch(buffer, INSERT_BATCH_SIZE);
                successCount += buffer.size();
                buffer.clear();
            }

            progress.setTotalRows(processedCount);
            progress.complete(successCount, failCount);
            progressMap.put(taskId, progress);
            logger.info("税务已缴导入完成, taskId: {}, 成功: {}, 失败: {}", taskId, successCount, failCount);
        } catch (Exception e) {
            logger.error("税务已缴导入失败, taskId: {}", taskId, e);
            progress.error(e.getMessage());
            progressMap.put(taskId, progress);
        } finally {
            try {
                Files.deleteIfExists(excelFile.toPath());
            } catch (Exception e) {
                logger.warn("清理税务已缴临时文件失败: {}", excelFile.getAbsolutePath(), e);
            }
        }
    }

    public ImportProgress getProgress(String taskId) {
        return progressMap.get(taskId);
    }

    public void removeProgress(String taskId) {
        progressMap.remove(taskId);
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean isPureNumber(String value) {
        return value != null && value.matches("\\d+");
    }

    private BigDecimal parseBigDecimal(String value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        String text = value.trim().replace(",", "");
        if (text.isEmpty()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(text);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private String getFileSuffix(String fileName) {
        if (fileName == null) {
            return ".xlsx";
        }
        String lowerFileName = fileName.toLowerCase();
        if (lowerFileName.endsWith(".xls")) {
            return ".xls";
        }
        return ".xlsx";
    }
}
