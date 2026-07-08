package com.ganen.tax.service.impl;

import com.ganen.tax.dto.ImportProgress;
import com.ganen.tax.dto.PageResult;
import com.ganen.tax.dto.TaxQueryRequest;
import com.ganen.tax.entity.TaxTuishuiAll;
import com.ganen.tax.mapper.TaxTuishuiAllMapper;
import com.ganen.tax.service.TaxTuishuiAllService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 退税全量名单 Service 实现
 * 分批从 v_yukou_all 获取去重id_card，逐批计算汇总并插入
 */
@Service
public class TaxTuishuiAllServiceImpl implements TaxTuishuiAllService {

    private static final Logger logger = LoggerFactory.getLogger(TaxTuishuiAllServiceImpl.class);

    /** 每批处理的身份证号数量 */
    private static final int BATCH_SIZE = 5000;

    @Autowired
    private TaxTuishuiAllMapper taxTuishuiAllMapper;

    /** 自注入以获取 @Async 代理 */
    @Lazy
    @Autowired
    private TaxTuishuiAllService self;

    /** 进度记录 */
    private final Map<String, ImportProgress> progressMap = new ConcurrentHashMap<>();

    @Override
    public String startCalculate() {
        String taskId = UUID.randomUUID().toString().replace("-", "");
        ImportProgress progress = ImportProgress.create(taskId);
        progress.setMessage("正在准备计算...");
        progressMap.put(taskId, progress);

        // 异步执行计算（通过自注入代理确保 @Async 生效）
        self.asyncCalculate(taskId);

        return taskId;
    }

    @Async
    public void asyncCalculate(String taskId) {
        ImportProgress progress = progressMap.get(taskId);
        int successCount = 0;
        int failCount = 0;
        int processedCount = 0;

        try {
            // 1. 清空表
            progress.setMessage("正在清空旧数据...");
            progressMap.put(taskId, progress);
            taxTuishuiAllMapper.truncateTable();
            logger.info("已清空 tax_tuishui_all 表");

            // 2. 分批获取身份证号并计算插入
            long offset = 0;
            int totalInserted = 0;

            while (true) {
                List<String> idCards = taxTuishuiAllMapper.selectDistinctIdCards(offset, BATCH_SIZE);
                if (idCards.isEmpty()) {
                    break;
                }

                try {
                    int inserted = taxTuishuiAllMapper.insertBatchCompute(idCards);
                    successCount += inserted;
                    totalInserted += inserted;
                    processedCount += idCards.size();

                    progress.update(processedCount, successCount, failCount);
                    progress.setMessage(String.format("正在计算... 已处理 %d 个身份证号，已插入 %d 条", processedCount, totalInserted));
                    progressMap.put(taskId, progress);

                    logger.info("批次处理完成: offset={}, idCards={}, inserted={}", offset, idCards.size(), inserted);
                } catch (Exception e) {
                    logger.error("批次插入失败: offset={}, size={}", offset, idCards.size(), e);
                    failCount += idCards.size();
                    progress.update(processedCount, successCount, failCount);
                    progressMap.put(taskId, progress);
                }

                offset += BATCH_SIZE;
            }

            progress.setTotalRows(processedCount);
            progress.complete(successCount, failCount);
            progressMap.put(taskId, progress);

            logger.info("退税全量计算完成, taskId: {}, 成功: {}, 失败: {}, 总插入: {}", taskId, successCount, failCount, totalInserted);

        } catch (Exception e) {
            logger.error("退税全量计算失败, taskId: {}", taskId, e);
            progress.error(e.getMessage());
            progressMap.put(taskId, progress);
        }
    }

    @Override
    public ImportProgress getProgress(String taskId) {
        return progressMap.get(taskId);
    }

    @Override
    public PageResult<TaxTuishuiAll> queryAllList(TaxQueryRequest request) {
        String userName = request == null ? null : request.getUserName();
        String idCard = request == null ? null : request.getIdCard();
        long pageNo = request == null || request.getPageNo() == null || request.getPageNo() < 1 ? 1 : request.getPageNo();
        long pageSize = request == null || request.getPageSize() == null || request.getPageSize() < 1 ? 10 : request.getPageSize();
        long offset = (pageNo - 1) * pageSize;
        long total = taxTuishuiAllMapper.countAllList(userName, idCard);
        List<TaxTuishuiAll> records = taxTuishuiAllMapper.queryAllList(userName, idCard, offset, pageSize);
        return PageResult.of(total, pageNo, pageSize, records);
    }

    @Override
    public List<TaxTuishuiAll> queryAllListForExport(TaxQueryRequest request) {
        String userName = request == null ? null : request.getUserName();
        String idCard = request == null ? null : request.getIdCard();
        long total = taxTuishuiAllMapper.countAllList(userName, idCard);
        return taxTuishuiAllMapper.queryAllList(userName, idCard, 0, total);
    }
}
