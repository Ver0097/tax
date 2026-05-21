package com.ganen.tax.service;

import com.ganen.tax.dto.PageResult;
import com.ganen.tax.dto.TaxQueryRequest;
import com.ganen.tax.entity.Tax;

import java.util.List;

public interface TaxService {

    int calculateRecoverInfo();

    PageResult<Tax> queryTaxList(TaxQueryRequest request);

    List<Tax> queryAllTaxList(TaxQueryRequest request);
}
