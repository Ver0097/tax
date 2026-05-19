package com.ganen.tax.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ganen.tax.entity.TaxUnpaid;
import com.ganen.tax.mapper.TaxUnpaidMapper;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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
public class ExcelImportService {
    
    @Autowired
    private TaxUnpaidMapper taxUnpaidMapper;
    
    private static final int SHEET_INDEX = 1;
    private static final int START_ROW = 2;
    
    private static final int COL_ID = 0;
    private static final int COL_USER_NAME = 2;
    private static final int COL_ID_CARD = 4;
    private static final int COL_PHONE = 8;
    private static final int COL_SHOULD_PAY = 24;
    
    @Transactional(rollbackFor = Exception.class)
    public int importUnpaidTaxData(MultipartFile file) throws IOException {
        List<TaxUnpaid> dataList = readExcelData(file);
        
        int successCount = 0;
        for (TaxUnpaid taxUnpaid : dataList) {
            if (taxUnpaid.getIdCard() == null || taxUnpaid.getIdCard().isEmpty()) {
                continue;
            }
            
            LambdaQueryWrapper<TaxUnpaid> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(TaxUnpaid::getIdCard, taxUnpaid.getIdCard());
            TaxUnpaid existing = taxUnpaidMapper.selectOne(queryWrapper);
            
            if (existing != null) {
                taxUnpaid.setId(existing.getId());
                taxUnpaid.setUpdateTime(LocalDateTime.now());
                taxUnpaidMapper.updateById(taxUnpaid);
            } else {
                taxUnpaid.setCreateTime(LocalDateTime.now());
                taxUnpaid.setUpdateTime(LocalDateTime.now());
                taxUnpaidMapper.insert(taxUnpaid);
            }
            successCount++;
        }
        
        return successCount;
    }
    
    private List<TaxUnpaid> readExcelData(MultipartFile file) throws IOException {
        List<TaxUnpaid> dataList = new ArrayList<>();
        
        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {
            
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
                
                TaxUnpaid taxUnpaid = parseRow(row);
                if (taxUnpaid != null && taxUnpaid.getIdCard() != null && !taxUnpaid.getIdCard().isEmpty()) {
                    dataList.add(taxUnpaid);
                }
            }
        }
        
        return dataList;
    }
    
    private TaxUnpaid parseRow(Row row) {
        TaxUnpaid taxUnpaid = new TaxUnpaid();
        
        Integer id = getIntegerValue(row, COL_ID);
        String userName = getStringValue(row, COL_USER_NAME);
        String idCard = getStringValue(row, COL_ID_CARD);
        String phone = getStringValue(row, COL_PHONE);
        BigDecimal shouldPay = getBigDecimalValue(row, COL_SHOULD_PAY);
        
        taxUnpaid.setId(id);
        taxUnpaid.setUserName(userName != null ? userName : "");
        taxUnpaid.setIdCard(idCard != null ? idCard : "");
        taxUnpaid.setPhone(phone != null ? phone : "");
        taxUnpaid.setShouldPay(shouldPay != null ? shouldPay : BigDecimal.ZERO);
        taxUnpaid.setPreDeduct(BigDecimal.ZERO);
        taxUnpaid.setActualPay(BigDecimal.ZERO);
        taxUnpaid.setRecoverPay(BigDecimal.ZERO);
        taxUnpaid.setStatus(0);
        
        return taxUnpaid;
    }
    
    private String getStringValue(Row row, int cellIndex) {
        Cell cell = row.getCell(cellIndex);
        if (cell == null) {
            return null;
        }
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    double value = cell.getNumericCellValue();
                    if (value == (long) value) {
                        return String.valueOf((long) value);
                    } else {
                        return String.valueOf(value);
                    }
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return cell.getStringCellValue();
                } catch (Exception e) {
                    try {
                        return String.valueOf(cell.getNumericCellValue());
                    } catch (Exception ex) {
                        return null;
                    }
                }
            default:
                return null;
        }
    }
    
    private Integer getIntegerValue(Row row, int cellIndex) {
        Cell cell = row.getCell(cellIndex);
        if (cell == null) {
            return null;
        }
        
        try {
            switch (cell.getCellType()) {
                case NUMERIC:
                    return (int) cell.getNumericCellValue();
                case STRING:
                    String value = cell.getStringCellValue().trim();
                    if (value.isEmpty()) {
                        return null;
                    }
                    return Integer.parseInt(value);
                case FORMULA:
                    try {
                        return (int) cell.getNumericCellValue();
                    } catch (Exception e) {
                        String formulaValue = cell.getStringCellValue().trim();
                        if (formulaValue.isEmpty()) {
                            return null;
                        }
                        return Integer.parseInt(formulaValue);
                    }
                default:
                    return null;
            }
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    private BigDecimal getBigDecimalValue(Row row, int cellIndex) {
        Cell cell = row.getCell(cellIndex);
        if (cell == null) {
            return BigDecimal.ZERO;
        }
        
        try {
            switch (cell.getCellType()) {
                case NUMERIC:
                    return BigDecimal.valueOf(cell.getNumericCellValue());
                case STRING:
                    String value = cell.getStringCellValue().trim();
                    if (value.isEmpty()) {
                        return BigDecimal.ZERO;
                    }
                    return new BigDecimal(value);
                case FORMULA:
                    try {
                        return BigDecimal.valueOf(cell.getNumericCellValue());
                    } catch (Exception e) {
                        String formulaValue = cell.getStringCellValue().trim();
                        if (formulaValue.isEmpty()) {
                            return BigDecimal.ZERO;
                        }
                        return new BigDecimal(formulaValue);
                    }
                default:
                    return BigDecimal.ZERO;
            }
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
}