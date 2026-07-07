package com.ganen.tax.service;

import com.ganen.tax.entity.TaxTuishuiJ;
import com.ganen.tax.mapper.TaxTuishuiJMapper;
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

/**
 * 退税着急名单 Excel 导入服务
 * Excel格式：A列-姓名，B列-身份证号，C列-退税金额（第一行为表头）
 */
@Service
public class TaxTuishuiJImportService {

    private static final int SHEET_INDEX = 0;
    /** 数据从第2行开始（第1行是表头） */
    private static final int START_ROW = 1;

    /** A列：姓名 */
    private static final int COL_USER_NAME = 0;
    /** B列：身份证号 */
    private static final int COL_ID_CARD = 1;
    /** C列：退税金额 */
    private static final int COL_TS_AMOUNT = 2;

    @Autowired
    private TaxTuishuiJMapper taxTuishuiJMapper;

    @Transactional(rollbackFor = Exception.class)
    public int importTuishuiData(MultipartFile file) throws IOException {
        List<TaxTuishuiJ> dataList = readExcelData(file);
        LocalDateTime now = LocalDateTime.now();

        for (TaxTuishuiJ item : dataList) {
            item.setCreateTime(now);
            item.setUpdateTime(now);
            taxTuishuiJMapper.insert(item);
        }

        return dataList.size();
    }

    private List<TaxTuishuiJ> readExcelData(MultipartFile file) throws IOException {
        List<TaxTuishuiJ> dataList = new ArrayList<>();
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

                TaxTuishuiJ item = parseRow(row, formatter);
                // 身份证号为空则跳过
                if (item.getIdCard() != null && !item.getIdCard().isEmpty()) {
                    dataList.add(item);
                }
            }
        }

        return dataList;
    }

    private TaxTuishuiJ parseRow(Row row, DataFormatter formatter) {
        TaxTuishuiJ item = new TaxTuishuiJ();
        item.setUserName(getStringValue(row, COL_USER_NAME, formatter));
        item.setIdCard(getStringValue(row, COL_ID_CARD, formatter));
        item.setTsAmount(getBigDecimalValue(row, COL_TS_AMOUNT, formatter));
        // 以下字段初始化为默认值（phone将在计算步骤中从v_yukou_all更新）
        item.setPhone("");
        item.setPreDeduct(BigDecimal.ZERO);
        item.setActualPay(BigDecimal.ZERO);
        item.setDiffAmount(BigDecimal.ZERO);
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
