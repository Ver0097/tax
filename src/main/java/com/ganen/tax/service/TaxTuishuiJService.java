package com.ganen.tax.service;

import com.ganen.tax.dto.PageResult;
import com.ganen.tax.dto.TaxQueryRequest;
import com.ganen.tax.entity.TaxTuishuiJ;

import java.util.List;

/**
 * 退税着急名单 Service
 */
public interface TaxTuishuiJService {

    /**
     * 计算退税着急名单的预扣金额、实缴金额、差额及相关信息
     */
    int calculateTuishuiInfo();

    /**
     * 分页查询退税着急名单
     */
    PageResult<TaxTuishuiJ> queryTuishuiList(TaxQueryRequest request);

    /**
     * 查询全部退税着急名单（用于导出）
     */
    List<TaxTuishuiJ> queryAllTuishuiList(TaxQueryRequest request);
}
