package com.ganen.tax.service.impl;

import com.ganen.tax.dto.PageResult;
import com.ganen.tax.dto.TaxQueryRequest;
import com.ganen.tax.entity.Tax;
import com.ganen.tax.mapper.TaxMapper;
import com.ganen.tax.service.TaxService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TaxServiceImpl implements TaxService {

    @Autowired
    private TaxMapper taxMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int calculateRecoverInfo() {
        return taxMapper.calculateRecoverInfo();
    }

    @Override
    public PageResult<Tax> queryTaxList(TaxQueryRequest request) {
        String userName = request == null ? null : request.getUserName();
        String idCard = request == null ? null : request.getIdCard();
        long pageNo = request == null || request.getPageNo() == null || request.getPageNo() < 1 ? 1 : request.getPageNo();
        long pageSize = request == null || request.getPageSize() == null || request.getPageSize() < 1 ? 10 : request.getPageSize();
        long offset = (pageNo - 1) * pageSize;
        long total = taxMapper.countTaxList(userName, idCard);
        List<Tax> records = taxMapper.queryTaxList(userName, idCard, offset, pageSize);
        return PageResult.of(total, pageNo, pageSize, records);
    }
}
