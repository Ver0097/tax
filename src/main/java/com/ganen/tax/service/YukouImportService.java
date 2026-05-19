package com.ganen.tax.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ganen.tax.dto.ImportProgress;
import com.ganen.tax.entity.YukouInfo;
import com.ganen.tax.mapper.YukouInfoMapper;
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
    
    @Autowired
    private YukouInfoMapper yukouInfoMapper;
    
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
                
                for (YukouInfo info : batch) {
                    if (info.getIdCard() == null || info.getIdCard().isEmpty()) {
                        failCount.incrementAndGet();
                        continue;
                    }
                    
                    try {
                        yukouInfoMapper.insert(info);
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        logger.error("插入数据失败, 身份证: {}", info.getIdCard(), e);
                        failCount.incrementAndGet();
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