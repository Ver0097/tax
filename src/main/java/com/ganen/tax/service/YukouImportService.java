package com.ganen.tax.service;

import com.ganen.tax.dto.ImportProgress;
import com.ganen.tax.entity.YukouInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class YukouImportService {

    private static final Logger logger = LoggerFactory.getLogger(YukouImportService.class);

    private static final int PARSE_BATCH_SIZE = 5000;
    private static final int INSERT_BATCH_SIZE = 1000;

    @Autowired
    private YukouInfoService yukouInfoService;

    @Autowired
    private ApplicationContext applicationContext;

    private final Map<String, ImportProgress> progressMap = new ConcurrentHashMap<>();

    public String startImport(MultipartFile file) {
        String taskId = UUID.randomUUID().toString().replace("-", "");
        ImportProgress progress = ImportProgress.create(taskId);
        progressMap.put(taskId, progress);

        try {
            Path tempFile = Files.createTempFile("yukou_import_", ".xlsx");
            file.transferTo(tempFile.toFile());
            applicationContext.getBean(YukouImportService.class).asyncImport(tempFile.toString(), taskId);
        } catch (Exception e) {
            progressMap.remove(taskId);
            throw new RuntimeException(e.getMessage(), e);
        }

        return taskId;
    }

    @Async
    public void asyncImport(String filePath, String taskId) {
        ImportProgress progress = progressMap.get(taskId);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        AtomicInteger processedCount = new AtomicInteger(0);

        File excelFile = new File(filePath);
        try {
            YukouExcelSaxParser parser = new YukouExcelSaxParser();

            parser.parse(excelFile, batch -> {
                processedCount.addAndGet(batch.size());

                if (!batch.isEmpty()) {
                    try {
                        yukouInfoService.saveBatch(batch, INSERT_BATCH_SIZE);
                        successCount.addAndGet(batch.size());
                    } catch (Exception e) {
                        logger.error("批量插入数据失败，批次大小: {}", batch.size(), e);
                        failCount.addAndGet(batch.size());
                    }
                }

                progress.update(processedCount.get(), successCount.get(), failCount.get());
                progressMap.put(taskId, progress);

            }, PARSE_BATCH_SIZE);
            
            progress.setTotalRows(processedCount.get());
            progress.complete(successCount.get(), failCount.get());
            progressMap.put(taskId, progress);
            
            logger.info("导入完成, taskId: {}, 成功: {}, 失败: {}", taskId, successCount.get(), failCount.get());
            
        } catch (Exception e) {
            logger.error("导入失败, taskId: {}", taskId, e);
            progress.error(e.getMessage());
            progressMap.put(taskId, progress);
        } finally {
            try {
                Files.deleteIfExists(excelFile.toPath());
            } catch (Exception e) {
                logger.warn("清理临时文件失败: {}", excelFile.getAbsolutePath(), e);
            }
        }
    }
    
    public ImportProgress getProgress(String taskId) {
        return progressMap.get(taskId);
    }
    
    public void removeProgress(String taskId) {
        progressMap.remove(taskId);
    }
}
