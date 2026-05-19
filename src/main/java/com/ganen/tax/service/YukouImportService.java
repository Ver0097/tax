package com.ganen.tax.service;

import com.ganen.tax.dto.ImportProgress;
import com.ganen.tax.entity.YukouInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class YukouImportService {
    
    private static final Logger logger = LoggerFactory.getLogger(YukouImportService.class);
    
    private static final int BATCH_SIZE = 5000;
    private static final int INSERT_BATCH_SIZE = 1000;
    
    @Autowired
    private YukouInfoService yukouInfoService;
    
    private final Map<String, ImportProgress> progressMap = new ConcurrentHashMap<>();
    
    public String startImport(MultipartFile file) {
        String taskId = UUID.randomUUID().toString().replace("-", "");
        ImportProgress progress = ImportProgress.create(taskId);
        progressMap.put(taskId, progress);
        
        asyncImport(file, taskId);
        
        return taskId;
    }
    
    @Async
    public void asyncImport(MultipartFile file, String taskId) {
        ImportProgress progress = progressMap.get(taskId);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        AtomicInteger processedCount = new AtomicInteger(0);
        
        try {
            YukouExcelSaxParser parser = new YukouExcelSaxParser();
            
            InputStream inputStream = file.getInputStream();
            
            parser.parse(inputStream, batch -> {
                processedCount.addAndGet(batch.size());
                
                List<YukouInfo> validList = new ArrayList<>();
                for (YukouInfo info : batch) {
                    if (info.getIdCard() != null && !info.getIdCard().isEmpty()) {
                        validList.add(info);
                    } else {
                        failCount.incrementAndGet();
                    }
                }
                
                if (!validList.isEmpty()) {
                    try {
                        yukouInfoService.saveBatch(validList, INSERT_BATCH_SIZE);
                        successCount.addAndGet(validList.size());
                    } catch (Exception e) {
                        logger.error("批量插入数据失败", e);
                        failCount.addAndGet(validList.size());
                    }
                }
                
                progress.update(processedCount.get(), successCount.get(), failCount.get());
                progressMap.put(taskId, progress);
                
            }, BATCH_SIZE);
            
            inputStream.close();
            
            progress.setTotalRows(processedCount.get());
            progress.complete(successCount.get(), failCount.get());
            progressMap.put(taskId, progress);
            
            logger.info("导入完成, taskId: {}, 成功: {}, 失败: {}", taskId, successCount.get(), failCount.get());
            
        } catch (Exception e) {
            logger.error("导入失败, taskId: {}", taskId, e);
            progress.error(e.getMessage());
            progressMap.put(taskId, progress);
        }
    }
    
    public ImportProgress getProgress(String taskId) {
        return progressMap.get(taskId);
    }
    
    public void removeProgress(String taskId) {
        progressMap.remove(taskId);
    }
}