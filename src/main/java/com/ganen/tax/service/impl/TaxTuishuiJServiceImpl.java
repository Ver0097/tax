package com.ganen.tax.service.impl;

import com.ganen.tax.dto.PageResult;
import com.ganen.tax.dto.TaxQueryRequest;
import com.ganen.tax.entity.TaxTuishuiJ;
import com.ganen.tax.mapper.TaxTuishuiJMapper;
import com.ganen.tax.service.TaxTuishuiJService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 退税着急名单 Service 实现
 */
@Service
public class TaxTuishuiJServiceImpl implements TaxTuishuiJService {

    @Autowired
    private TaxTuishuiJMapper taxTuishuiJMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int calculateTuishuiInfo() {
        return taxTuishuiJMapper.calculateTuishuiInfo();
    }

    @Override
    public PageResult<TaxTuishuiJ> queryTuishuiList(TaxQueryRequest request) {
        String userName = request == null ? null : request.getUserName();
        String idCard = request == null ? null : request.getIdCard();
        long pageNo = request == null || request.getPageNo() == null || request.getPageNo() < 1 ? 1 : request.getPageNo();
        long pageSize = request == null || request.getPageSize() == null || request.getPageSize() < 1 ? 10 : request.getPageSize();
        long offset = (pageNo - 1) * pageSize;
        long total = taxTuishuiJMapper.countTuishuiList(userName, idCard);
        List<TaxTuishuiJ> records = taxTuishuiJMapper.queryTuishuiList(userName, idCard, offset, pageSize);
        return PageResult.of(total, pageNo, pageSize, records);
    }

    @Override
    public List<TaxTuishuiJ> queryAllTuishuiList(TaxQueryRequest request) {
        String userName = request == null ? null : request.getUserName();
        String idCard = request == null ? null : request.getIdCard();
        long total = taxTuishuiJMapper.countTuishuiList(userName, idCard);
        return taxTuishuiJMapper.queryTuishuiList(userName, idCard, 0, total);
    }
}
