package com.ganen.tax.service;

import com.ganen.tax.entity.YukouInfo;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackageAccess;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.util.CellAddress;
import org.apache.poi.util.IOUtils;
import org.apache.poi.util.XMLHelper;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler;
import org.apache.poi.xssf.model.StylesTable;
import org.apache.poi.xssf.model.SharedStrings;
import org.apache.poi.xssf.usermodel.XSSFComment;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import java.io.File;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class YukouExcelSaxParser {
    
    private static final int COL_MERCHANT = 0;
    private static final int COL_TAX_AREA = 2;
    private static final int COL_TAX_AMOUNT = 6;
    private static final int COL_PAYEE = 14;
    private static final int COL_ID_CARD = 15;
    private static final int COL_PHONE = 16;
    private static final int COL_SALE = 20;
    private static final int COL_CUSTOMER_SERVICE = 21;
    private static final int COL_CHANNEL = 22;
    
    private static final int HEADER_ROWS = 3;
    
    private final DataFormatter formatter = new DataFormatter();
    
    public void parse(File excelFile, Consumer<List<YukouInfo>> batchConsumer, int batchSize) throws Exception {
        System.setProperty("jdk.xml.entityExpansionLimit", "0");
        ZipSecureFile.setMinInflateRatio(0);
        ZipSecureFile.setMaxEntrySize(Long.MAX_VALUE);
        ZipSecureFile.setMaxTextSize(Long.MAX_VALUE);
        IOUtils.setByteArrayMaxOverride(Integer.MAX_VALUE);

        OPCPackage opcPackage = OPCPackage.open(excelFile, PackageAccess.READ);
        XSSFReader xssfReader = new XSSFReader(opcPackage);

        SharedStrings sharedStrings = xssfReader.getSharedStringsTable();
        StylesTable stylesTable = xssfReader.getStylesTable();

        XSSFReader.SheetIterator sheetIterator = (XSSFReader.SheetIterator) xssfReader.getSheetsData();
        int sheetIndex = 0;

        while (sheetIterator.hasNext()) {
            try (InputStream sheetStream = sheetIterator.next()) {
                SheetHandler sheetHandler = new SheetHandler(batchConsumer, batchSize, sheetIndex);

                XMLReader xmlReader = XMLHelper.newXMLReader();
                XSSFSheetXMLHandler xssfSheetXMLHandler = new XSSFSheetXMLHandler(
                        stylesTable, sharedStrings, sheetHandler, formatter, false);
                xmlReader.setContentHandler(xssfSheetXMLHandler);

                InputSource inputSource = new InputSource(sheetStream);
                xmlReader.parse(inputSource);

                sheetHandler.flush();
                sheetIndex++;
            }
        }

        opcPackage.close();
    }
    
    private class SheetHandler implements XSSFSheetXMLHandler.SheetContentsHandler {
        private final Consumer<List<YukouInfo>> batchConsumer;
        private final int batchSize;
        private final int sheetIndex;
        
        private int currentRow = -1;
        private List<YukouInfo> currentBatch = new ArrayList<>();
        private String[] currentRowValues = new String[30];
        
        public SheetHandler(Consumer<List<YukouInfo>> batchConsumer, int batchSize, int sheetIndex) {
            this.batchConsumer = batchConsumer;
            this.batchSize = batchSize;
            this.sheetIndex = sheetIndex;
        }
        
        @Override
        public void startRow(int rowNum) {
            currentRow = rowNum;
            currentRowValues = new String[30];
        }
        
        @Override
        public void endRow(int rowNum) {
            if (rowNum < HEADER_ROWS) {
                return;
            }
            
            YukouInfo yukouInfo = parseRowData();
            if (yukouInfo != null && yukouInfo.getIdCard() != null && !yukouInfo.getIdCard().isEmpty()) {
                currentBatch.add(yukouInfo);
                
                if (currentBatch.size() >= batchSize) {
                    batchConsumer.accept(new ArrayList<>(currentBatch));
                    currentBatch.clear();
                }
            }
        }

        public void flush() {
            if (!currentBatch.isEmpty()) {
                batchConsumer.accept(new ArrayList<>(currentBatch));
                currentBatch.clear();
            }
        }
        
        @Override
        public void cell(String cellReference, String formattedValue, XSSFComment comment) {
            if (cellReference == null) {
                return;
            }
            
            CellAddress cellAddress = new CellAddress(cellReference);
            int col = cellAddress.getColumn();
            
            if (col < currentRowValues.length) {
                currentRowValues[col] = formattedValue != null ? formattedValue.trim() : "";
            }
        }
        
        @Override
        public void headerFooter(String text, boolean isHeader, String tagName) {
        }
        
        private YukouInfo parseRowData() {
            YukouInfo yukouInfo = new YukouInfo();
            
            yukouInfo.setMerchant(getValue(COL_MERCHANT));
            yukouInfo.setTaxArea(getValue(COL_TAX_AREA));
            yukouInfo.setTaxAmount(parseBigDecimal(getValue(COL_TAX_AMOUNT)));
            yukouInfo.setPayee(getValue(COL_PAYEE));
            yukouInfo.setIdCard(getValue(COL_ID_CARD));
            yukouInfo.setPhone(getValue(COL_PHONE));
            yukouInfo.setSale(getValue(COL_SALE));
            yukouInfo.setCustomerService(getValue(COL_CUSTOMER_SERVICE));
            yukouInfo.setChannel(getValue(COL_CHANNEL));
            yukouInfo.setOrderSource(0);
            
            return yukouInfo;
        }
        
        private String getValue(int col) {
            if (col < currentRowValues.length) {
                String value = currentRowValues[col];
                return value != null ? value : "";
            }
            return "";
        }
        
        private BigDecimal parseBigDecimal(String value) {
            if (value == null || value.isEmpty()) {
                return BigDecimal.ZERO;
            }
            try {
                return new BigDecimal(value);
            } catch (NumberFormatException e) {
                return BigDecimal.ZERO;
            }
        }
    }
}
