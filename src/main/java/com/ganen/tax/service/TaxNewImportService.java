package com.ganen.tax.service;

import com.ganen.tax.entity.TaxNewUnpaid;
import com.ganen.tax.mapper.TaxNewUnpaidMapper;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class TaxNewImportService {

    private static final int SHEET_INDEX = 1;
    private static final int START_ROW = 2;
    private static final int COL_USER_NAME = 2;
    private static final int COL_ID_CARD = 4;
    private static final int COL_PHONE = 8;
    private static final int COL_WITHHOLDING_AGENT = 13;
    private static final int COL_SHOULD_PAY = 24;

    @Autowired
    private TaxNewUnpaidMapper taxNewUnpaidMapper;

    @Transactional(rollbackFor = Exception.class)
    public int importNewUnpaidTaxData(MultipartFile file) throws IOException {
        List<TaxNewUnpaid> dataList = readExcelData(file);
        LocalDateTime now = LocalDateTime.now();

        for (TaxNewUnpaid item : dataList) {
            item.setCreateTime(now);
            item.setUpdateTime(now);
            taxNewUnpaidMapper.insert(item);
        }

        return dataList.size();
    }

    private List<TaxNewUnpaid> readExcelData(MultipartFile file) throws IOException {
        List<TaxNewUnpaid> dataList = new ArrayList<>();
        DataFormatter formatter = new DataFormatter();

        try (InputStream is = file.getInputStream(); Workbook workbook = WorkbookFactory.create(is)) {
            Sheet sheet = workbook.getSheetAt(SHEET_INDEX);
            if (sheet == null) {
                throw new RuntimeException("Excel文件中没有找到第" + (SHEET_INDEX + 1) + "个sheet页");
            }

            int lastRowNum = sheet.getLastRowNum();
            for (int i = START_ROW; i <= lastRowNum; i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }

                dataList.add(parseRow(row, formatter));
            }
        }

        return dataList;
    }

    private TaxNewUnpaid parseRow(Row row, DataFormatter formatter) {
        TaxNewUnpaid item = new TaxNewUnpaid();
        item.setUserName(getStringValue(row, COL_USER_NAME, formatter));
        item.setIdCard(getStringValue(row, COL_ID_CARD, formatter));
        item.setPhone(getStringValue(row, COL_PHONE, formatter));
        item.setWithholdingAgent(getStringValue(row, COL_WITHHOLDING_AGENT, formatter));
        item.setShouldPay(getBigDecimalValue(row, COL_SHOULD_PAY, formatter));
        item.setPreDeduct(BigDecimal.ZERO);
        item.setActualPay(BigDecimal.ZERO);
        item.setDiffAmount(BigDecimal.ZERO);
        item.setRecoverPay(BigDecimal.ZERO);
        item.setTaxArea("");
        item.setMerchant("");
        item.setChannel("");
        item.setSale("");
        item.setCustomerService("");
        item.setStatus(0);
        return item;
    }

    private String getStringValue(Row row, int cellIndex, DataFormatter formatter) {
        return formatter.formatCellValue(row.getCell(cellIndex)).trim();
    }

    private BigDecimal getBigDecimalValue(Row row, int cellIndex, DataFormatter formatter) {
        String value = formatter.formatCellValue(row.getCell(cellIndex)).trim().replace(",", "");
        if (value.isEmpty()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(value);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }
}
