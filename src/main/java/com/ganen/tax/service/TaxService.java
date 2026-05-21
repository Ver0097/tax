package com.ganen.tax.service;

import com.ganen.tax.dto.PageResult;
import com.ganen.tax.dto.TaxQueryRequest;
import com.ganen.tax.entity.Tax;

public interface TaxService {

    int calculateRecoverInfo();

    PageResult<Tax> queryTaxList(TaxQueryRequest request);
}
