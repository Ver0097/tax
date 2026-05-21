package com.ganen.tax.service;

import com.ganen.tax.dto.TaxQueryRequest;
import com.ganen.tax.entity.Tax;

import java.util.List;

public interface TaxService {

    int calculateRecoverInfo();

    List<Tax> queryTaxList(TaxQueryRequest request);
}
