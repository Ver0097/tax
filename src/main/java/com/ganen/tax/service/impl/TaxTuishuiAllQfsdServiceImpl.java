package com.ganen.tax.service.impl;

import com.ganen.tax.dto.ImportProgress;
import com.ganen.tax.dto.PageResult;
import com.ganen.tax.dto.TaxQueryRequest;
import com.ganen.tax.entity.TaxTuishuiAllQfsd;
import com.ganen.tax.mapper.TaxTuishuiAllQfsdMapper;
import com.ganen.tax.service.TaxTuishuiAllQfsdService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 退税全量名单（区分税地） Service 实现
 * 按 id_card + tax_area 分组，同一个人在不同税地下拆分为多条记录
 */
@Service
public class TaxTuishuiAllQfsdServiceImpl implements TaxTuishuiAllQfsdService {

    private static final Logger logger = LoggerFactory.getLogger(TaxTuishuiAllQfsdServiceImpl.class);

    private static final int BATCH_SIZE = 5000;

    /** 已知税地不匹配的34个身份证号 */
    private static final List<String> ORPHAN_ID_CARDS = Arrays.asList(
        "110111199606264029", "130105198806290629", "130582199401280413",
        "220422198503010022", "310101198108280077", "310102198312164520",
        "310104198706270847", "310107197105270029", "31010719891124396x",
        "310107199512047210", "31010720001208212x", "310108198603260027",
        "31010819871228284x", "310109198311220054", "310109198707024025",
        "310109198910111546", "31023019660205396x", "320311198706245525",
        "320684199510302961", "320923198007195491", "350583199405073769",
        "370684198808244834", "412728199811042815", "420102196810203525",
        "421083199301081627", "430603199409070016", "510106198102183531",
        "510128197710145823", "510902197105173631", "511022197411223446",
        "513321199612230029", "610422198412131729", "622421198911140029",
        "632121199202160043"
    );

    @Autowired
    private TaxTuishuiAllQfsdMapper taxTuishuiAllQfsdMapper;

    /** 自注入以获取 @Async 代理 */
    @Lazy
    @Autowired
    private TaxTuishuiAllQfsdService self;

    private final Map<String, ImportProgress> progressMap = new ConcurrentHashMap<>();

    @Override
    public String startCalculate() {
        String taskId = UUID.randomUUID().toString().replace("-", "");
        ImportProgress progress = ImportProgress.create(taskId);
        progress.setMessage("正在准备计算...");
        progressMap.put(taskId, progress);

        self.asyncCalculate(taskId);

        return taskId;
    }

    @Async
    @Override
    public void asyncCalculate(String taskId) {
        ImportProgress progress = progressMap.get(taskId);
        int successCount = 0;
        int failCount = 0;
        int processedCount = 0;

        try {
            // 1. 清空表
            progress.setMessage("正在清空旧数据...");
            progressMap.put(taskId, progress);
            taxTuishuiAllQfsdMapper.truncateTable();
            logger.info("已清空 tax_tuishui_all_qfsd 表");

            // 2. 分批获取身份证号并计算插入（INSERT 内部按 id_card + tax_area 分组）
            long offset = 0;
            int totalInserted = 0;

            while (true) {
                List<String> idCards = taxTuishuiAllQfsdMapper.selectDistinctIdCards(offset, BATCH_SIZE);
                if (idCards.isEmpty()) {
                    break;
                }

                try {
                    int inserted = taxTuishuiAllQfsdMapper.insertBatchCompute(idCards);
                    successCount += inserted;
                    totalInserted += inserted;
                    processedCount += idCards.size();

                    progress.update(processedCount, successCount, failCount);
                    progress.setMessage(String.format("正在计算... 已处理 %d 个身份证号，已插入 %d 条（含税地拆分）", processedCount, totalInserted));
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

            // 3. 补入孤儿实缴记录：在 yijiao_info_qfsd 中有实缴、但 v_yukou_all 中无对应税地的记录
            progress.setMessage("正在补入孤儿实缴记录...");
            progressMap.put(taskId, progress);
            int orphanCount = taxTuishuiAllQfsdMapper.insertOrphanActualPay(ORPHAN_ID_CARDS);
            if (orphanCount > 0) {
                successCount += orphanCount;
                logger.info("补入孤儿实缴 {} 条", orphanCount);
            }

            progress.setTotalRows(processedCount);
            progress.complete(successCount, failCount);
            progressMap.put(taskId, progress);

            logger.info("退税全量(区分税地)计算完成, taskId: {}, 成功: {}, 失败: {}, 总插入: {}", taskId, successCount, failCount, totalInserted);

        } catch (Exception e) {
            logger.error("退税全量(区分税地)计算失败, taskId: {}", taskId, e);
            progress.error(e.getMessage());
            progressMap.put(taskId, progress);
        }
    }

    @Override
    public ImportProgress getProgress(String taskId) {
        return progressMap.get(taskId);
    }

    @Override
    public PageResult<TaxTuishuiAllQfsd> queryAllList(TaxQueryRequest request) {
        String userName = request == null ? null : request.getUserName();
        String idCard = request == null ? null : request.getIdCard();
        long pageNo = request == null || request.getPageNo() == null || request.getPageNo() < 1 ? 1 : request.getPageNo();
        long pageSize = request == null || request.getPageSize() == null || request.getPageSize() < 1 ? 10 : request.getPageSize();
        long offset = (pageNo - 1) * pageSize;
        long total = taxTuishuiAllQfsdMapper.countAllList(userName, idCard);
        List<TaxTuishuiAllQfsd> records = taxTuishuiAllQfsdMapper.queryAllList(userName, idCard, offset, pageSize);
        return PageResult.of(total, pageNo, pageSize, records);
    }

    @Override
    public List<TaxTuishuiAllQfsd> queryAllListForExport(TaxQueryRequest request) {
        String userName = request == null ? null : request.getUserName();
        String idCard = request == null ? null : request.getIdCard();
        long total = taxTuishuiAllQfsdMapper.countAllList(userName, idCard);
        return taxTuishuiAllQfsdMapper.queryAllList(userName, idCard, 0, total);
    }
}
