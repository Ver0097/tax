package com.ganen.tax.service;

import com.ganen.tax.dto.ImportProgress;
import com.ganen.tax.entity.YijiaoInfoQfsd;
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

/**
 * 税务已缴数据导入服务（区分税地）
 * 复刻任务5 YijiaoImportService 逻辑，新增从第3行提取税地信息
 */
@Service
public class YijiaoQfsdImportService {

    private static final Logger logger = LoggerFactory.getLogger(YijiaoQfsdImportService.class);

    private static final int INSERT_BATCH_SIZE = 1000;
    /** Excel第3行（0-indexed = 2），用于提取扣缴义务人名称作为tax_area */
    private static final int TAX_AREA_ROW = 2;
    /** 数据从第9行开始（0-indexed = 8） */
    private static final int START_ROW_INDEX = 8;
    /** A列：行号标识，用于判断是否为有效数据行 */
    private static final int COL_ROW_FLAG = 0;
    /** B列：姓名 */
    private static final int COL_USER_NAME = 1;
    /** D列：身份证号 */
    private static final int COL_ID_CARD = 3;
    /** AO列：已缴金额 */
    private static final int COL_PAID_AMOUNT = 40;

    @Autowired
    private YijiaoInfoQfsdService yijiaoInfoQfsdService;

    @Autowired
    private ApplicationContext applicationContext;

    private final Map<String, ImportProgress> progressMap = new ConcurrentHashMap<>();

    /**
     * 启动异步导入，返回任务ID
     */
    public String startImport(MultipartFile file) {
        String taskId = UUID.randomUUID().toString().replace("-", "");
        ImportProgress progress = ImportProgress.create(taskId);
        progressMap.put(taskId, progress);

        try {
            String originalFilename = file.getOriginalFilename();
            String suffix = getFileSuffix(originalFilename);
            Path tempFile = Files.createTempFile("yijiao_qfsd_import_", suffix);
            file.transferTo(tempFile.toFile());
            applicationContext.getBean(YijiaoQfsdImportService.class).asyncImport(tempFile.toString(), taskId);
        } catch (Exception e) {
            progressMap.remove(taskId);
            throw new RuntimeException(e.getMessage(), e);
        }

        return taskId;
    }

    /**
     * 异步导入处理
     */
    @Async
    public void asyncImport(String filePath, String taskId) {
        ImportProgress progress = progressMap.get(taskId);
        int successCount = 0;
        int failCount = 0;
        int processedCount = 0;

        File excelFile = new File(filePath);
        List<YijiaoInfoQfsd> buffer = new ArrayList<>(INSERT_BATCH_SIZE);
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

            // 从第3行提取税地信息（扣缴义务人名称：xxx → 取xxx）
            String taxArea = extractTaxArea(sheet, formatter);
            logger.info("提取税地信息: {}", taxArea);

            int lastRowNum = sheet.getLastRowNum();
            for (int i = START_ROW_INDEX; i <= lastRowNum; i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }

                // A列不是纯数字 → 终止导入
                String rowFlag = trim(formatter.formatCellValue(row.getCell(COL_ROW_FLAG)));
                if (!isPureNumber(rowFlag)) {
                    break;
                }

                processedCount++;

                // AO列值为0 → 跳过不落库
                BigDecimal paidAmount = parseBigDecimal(formatter.formatCellValue(row.getCell(COL_PAID_AMOUNT)));
                if (paidAmount.compareTo(BigDecimal.ZERO) == 0) {
                    failCount++;
                    progress.setTotalRows(processedCount);
                    progress.update(processedCount, successCount, failCount);
                    progressMap.put(taskId, progress);
                    continue;
                }

                YijiaoInfoQfsd info = new YijiaoInfoQfsd();
                info.setPaidAmount(paidAmount);
                info.setUserName(trim(formatter.formatCellValue(row.getCell(COL_USER_NAME))));
                info.setIdCard(trim(formatter.formatCellValue(row.getCell(COL_ID_CARD))));
                info.setTaxArea(taxArea);
                buffer.add(info);

                if (buffer.size() >= INSERT_BATCH_SIZE) {
                    yijiaoInfoQfsdService.saveBatch(buffer, INSERT_BATCH_SIZE);
                    successCount += buffer.size();
                    buffer.clear();
                }

                progress.setTotalRows(processedCount);
                progress.update(processedCount, successCount, failCount);
                progressMap.put(taskId, progress);
            }

            // 处理剩余的缓冲数据
            if (!buffer.isEmpty()) {
                yijiaoInfoQfsdService.saveBatch(buffer, INSERT_BATCH_SIZE);
                successCount += buffer.size();
                buffer.clear();
            }

            progress.setTotalRows(processedCount);
            progress.complete(successCount, failCount);
            progressMap.put(taskId, progress);
            logger.info("税务已缴(区分税地)导入完成, taskId: {}, 税地: {}, 成功: {}, 失败: {}", taskId, taxArea, successCount, failCount);
        } catch (Exception e) {
            logger.error("税务已缴(区分税地)导入失败, taskId: {}", taskId, e);
            progress.error(e.getMessage());
            progressMap.put(taskId, progress);
        } finally {
            try {
                Files.deleteIfExists(excelFile.toPath());
            } catch (Exception e) {
                logger.warn("清理税务已缴(区分税地)临时文件失败: {}", excelFile.getAbsolutePath(), e);
            }
        }
    }

    /**
     * 从Excel第3行提取税地信息
     * 格式："扣缴义务人名称：天津慧速信息科技有限公司" → 取"天津慧速信息科技有限公司"
     */
    private String extractTaxArea(Sheet sheet, DataFormatter formatter) {
        Row row = sheet.getRow(TAX_AREA_ROW);
        if (row == null) {
            return "";
        }
        // 遍历该行所有单元格，找到含冒号的内容并提取冒号后的值
        for (int i = 0; i <= row.getLastCellNum(); i++) {
            String cellValue = trim(formatter.formatCellValue(row.getCell(i)));
            if (cellValue.isEmpty()) {
                continue;
            }
            // 匹配中文冒号"："或英文冒号":"
            int colonIdx = cellValue.indexOf("：");
            if (colonIdx == -1) {
                colonIdx = cellValue.indexOf(":");
            }
            if (colonIdx > 0 && colonIdx < cellValue.length() - 1) {
                return cellValue.substring(colonIdx + 1).trim();
            }
        }
        return "";
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
