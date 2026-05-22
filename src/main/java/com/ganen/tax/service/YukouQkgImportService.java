package com.ganen.tax.service;

import com.ganen.tax.entity.YukouQkgInfo;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class YukouQkgImportService {

    private static final int INSERT_BATCH_SIZE = 1000;

    private static final int COL_PAYEE = 0;
    private static final int COL_ID_CARD = 1;
    private static final int COL_PHONE = 2;
    private static final int COL_TAX_AMOUNT = 8;
    private static final int COL_MERCHANT = 10;
    private static final int COL_TAX_AREA = 19;

    private static final String FIXED_CHANNEL = "北京趣开工科技有限公司";
    private static final String FIXED_SALE = "令狐";
    private static final String FIXED_CUSTOMER_SERVICE = "苗苗";

    @Autowired
    private YukouQkgInfoService yukouQkgInfoService;

    public int importQkg(MultipartFile file) throws Exception {
        DataFormatter formatter = new DataFormatter();
        List<YukouQkgInfo> buffer = new ArrayList<>(INSERT_BATCH_SIZE);
        int success = 0;

        try (InputStream is = file.getInputStream(); Workbook workbook = WorkbookFactory.create(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                return 0;
            }

            int lastRowNum = sheet.getLastRowNum();
            for (int i = 1; i <= lastRowNum; i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }

                BigDecimal taxAmount = parseBigDecimal(formatter.formatCellValue(row.getCell(COL_TAX_AMOUNT)));

                YukouQkgInfo info = new YukouQkgInfo();
                info.setPayee(trim(formatter.formatCellValue(row.getCell(COL_PAYEE))));
                info.setIdCard(trim(formatter.formatCellValue(row.getCell(COL_ID_CARD))));
                info.setPhone(trim(formatter.formatCellValue(row.getCell(COL_PHONE))));
                info.setMerchant(trim(formatter.formatCellValue(row.getCell(COL_MERCHANT))));
                info.setTaxArea(trim(formatter.formatCellValue(row.getCell(COL_TAX_AREA))));
                info.setTaxAmount(taxAmount);
                info.setChannel(FIXED_CHANNEL);
                info.setSale(FIXED_SALE);
                info.setCustomerService(FIXED_CUSTOMER_SERVICE);
                info.setOrderSource(1);

                buffer.add(info);
                if (buffer.size() >= INSERT_BATCH_SIZE) {
                    yukouQkgInfoService.saveBatch(buffer, INSERT_BATCH_SIZE);
                    success += buffer.size();
                    buffer.clear();
                }
            }
        }

        if (!buffer.isEmpty()) {
            yukouQkgInfoService.saveBatch(buffer, INSERT_BATCH_SIZE);
            success += buffer.size();
        }

        return success;
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private BigDecimal parseBigDecimal(String value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        String v = value.trim();
        if (v.isEmpty()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(v);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }
}
