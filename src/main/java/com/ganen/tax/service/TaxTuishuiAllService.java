package com.ganen.tax.service;

import com.ganen.tax.dto.ImportProgress;
import com.ganen.tax.dto.PageResult;
import com.ganen.tax.dto.TaxQueryRequest;
import com.ganen.tax.entity.TaxTuishuiAll;

import java.util.List;

/**
 * 退税全量名单 Service
 */
public interface TaxTuishuiAllService {

    /** 启动异步计算任务，返回任务ID */
    String startCalculate();

    /** 异步执行批量计算（供内部代理调用） */
    void asyncCalculate(String taskId);

    /** 获取计算进度 */
    ImportProgress getProgress(String taskId);

    /** 分页查询 */
    PageResult<TaxTuishuiAll> queryAllList(TaxQueryRequest request);

    /** 查询全部（导出用） */
    List<TaxTuishuiAll> queryAllListForExport(TaxQueryRequest request);
}
