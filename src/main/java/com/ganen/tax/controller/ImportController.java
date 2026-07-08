package com.ganen.tax.controller;

import com.ganen.tax.common.Result;
import com.ganen.tax.dto.ImportProgress;
import com.ganen.tax.dto.PageResult;
import com.ganen.tax.dto.TaxQueryRequest;
import com.ganen.tax.service.ExcelImportService;
import com.ganen.tax.service.TaxNewImportService;
import com.ganen.tax.service.TaxService;
import com.ganen.tax.service.YijiaoImportService;
import com.ganen.tax.service.YukouImportService;
import com.ganen.tax.service.YukouJlImportService;
import com.ganen.tax.service.TaxTuishuiJImportService;
import com.ganen.tax.service.TaxTuishuiJService;
import com.ganen.tax.service.TaxTuishuiAllService;
import com.ganen.tax.service.YukouQkgImportService;
import com.ganen.tax.entity.Tax;
import com.ganen.tax.entity.TaxTuishuiJ;
import com.ganen.tax.entity.TaxTuishuiAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;


@Controller
public class ImportController {
    
    @Autowired
    private ExcelImportService excelImportService;

    @Autowired
    private TaxNewImportService taxNewImportService;

    @Autowired
    private TaxService taxService;
    
    @Autowired
    private YukouImportService yukouImportService;
    
    @Autowired
    private YukouQkgImportService yukouQkgImportService;

    @Autowired
    private YukouJlImportService yukouJlImportService;

    @Autowired
    private YijiaoImportService yijiaoImportService;

    @Autowired
    private TaxTuishuiJImportService taxTuishuiJImportService;

    @Autowired
    private TaxTuishuiJService taxTuishuiJService;

    @Autowired
    private TaxTuishuiAllService taxTuishuiAllService;

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/tax-query")
    public String taxQueryPage() {
        return "tax-query";
    }

    @GetMapping("/tuishui-query")
    public String tuishuiQueryPage() {
        return "tuishui-query";
    }

    @GetMapping("/tuishui-all-query")
    public String tuishuiAllQueryPage() {
        return "tuishui-all-query";
    }

    @PostMapping("/api/import/excel")
    @ResponseBody
    public Result<Integer> importExcel(
            @RequestParam("file") MultipartFile file,
            @RequestParam("importType") String importType) {
        try {
            if (file.isEmpty()) {
                return Result.error("请选择要导入的文件");
            }
            
            String fileName = file.getOriginalFilename();
            if (fileName == null || (!fileName.endsWith(".xlsx") && !fileName.endsWith(".xls"))) {
                return Result.error("请选择Excel文件（.xlsx或.xls格式）");
            }
            
            int count = excelImportService.importUnpaidTaxData(file);
            return Result.success("导入成功", count);
        } catch (Exception e) {
            return Result.error("导入失败：" + e.getMessage());
        }
    }

    @PostMapping("/api/import/new-tax")
    @ResponseBody
    public Result<Integer> importNewTax(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return Result.error("请选择要导入的文件");
            }

            String fileName = file.getOriginalFilename();
            if (fileName == null || (!fileName.endsWith(".xlsx") && !fileName.endsWith(".xls"))) {
                return Result.error("请选择Excel文件（.xlsx或.xls格式）");
            }

            int count = taxNewImportService.importNewUnpaidTaxData(file);
            return Result.success("导入成功", count);
        } catch (Exception e) {
            return Result.error("导入失败：" + e.getMessage());
        }
    }

    @PostMapping("/api/tax/calculate")
    @ResponseBody
    public Result<Integer> calculateTaxRecoverInfo() {
        try {
            int count = taxService.calculateRecoverInfo();
            return Result.success("计算成功", count);
        } catch (Exception e) {
            return Result.error("计算失败：" + e.getMessage());
        }
    }

    @PostMapping("/api/tax/query")
    @ResponseBody
    public Result<PageResult<Tax>> queryTaxList(@RequestBody(required = false) TaxQueryRequest request) {
        try {
            return Result.success(taxService.queryTaxList(request));
        } catch (Exception e) {
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    @PostMapping("/api/tax/export")
    public void exportTaxList(@RequestParam(value = "userName", required = false) String userName,
                              @RequestParam(value = "idCard", required = false) String idCard,
                              HttpServletResponse response) {
        Workbook workbook = new XSSFWorkbook();
        try {
            TaxQueryRequest request = new TaxQueryRequest();
            request.setUserName(userName);
            request.setIdCard(idCard);
            List<Tax> list = taxService.queryAllTaxList(request);
            Sheet sheet = workbook.createSheet("应追缴金额和相关信息");

            String[] headers = {"姓名", "身份证号", "联系电话", "扣缴义务人名称", "应补金额", "预扣金额", "实缴金额", "（预扣-实缴）金额", "追缴金额", "涉及税地", "涉及商户", "涉及渠道", "涉及销售", "涉及客服"};
            Row headerRow = sheet.createRow(0);
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            for (int i = 0; i < list.size(); i++) {
                Tax tax = list.get(i);
                Row row = sheet.createRow(i + 1);
                row.createCell(0).setCellValue(nullToEmpty(tax.getUserName()));
                row.createCell(1).setCellValue(nullToEmpty(tax.getIdCard()));
                row.createCell(2).setCellValue(nullToEmpty(tax.getPhone()));
                row.createCell(3).setCellValue(nullToEmpty(tax.getWithholdingAgent()));
                row.createCell(4).setCellValue(formatNumber(tax.getShouldPay()));
                row.createCell(5).setCellValue(formatNumber(tax.getPreDeduct()));
                row.createCell(6).setCellValue(formatNumber(tax.getActualPay()));
                row.createCell(7).setCellValue(formatNumber(tax.getDiffAmount()));
                row.createCell(8).setCellValue(formatNumber(tax.getRecoverPay()));
                row.createCell(9).setCellValue(nullToEmpty(tax.getTaxArea()));
                row.createCell(10).setCellValue(nullToEmpty(tax.getMerchant()));
                row.createCell(11).setCellValue(nullToEmpty(tax.getChannel()));
                row.createCell(12).setCellValue(nullToEmpty(tax.getSale()));
                row.createCell(13).setCellValue(nullToEmpty(tax.getCustomerService()));
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            String fileName = URLEncoder.encode("应追缴金额和相关信息", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + fileName + ".xlsx");

            try (OutputStream os = response.getOutputStream()) {
                workbook.write(os);
                os.flush();
            }
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            try {
                workbook.close();
            } catch (Exception ignored) {
            }
        }
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String formatNumber(java.math.BigDecimal value) {
        return value == null ? "" : value.toPlainString();
    }
    
    @PostMapping("/api/import/yukou")
    @ResponseBody
    public Result<String> importYukou(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return Result.error("请选择要导入的文件");
            }
            
            String fileName = file.getOriginalFilename();
            if (fileName == null || (!fileName.endsWith(".xlsx") && !fileName.endsWith(".xls"))) {
                return Result.error("请选择Excel文件（.xlsx或.xls格式）");
            }
            
            String taskId = yukouImportService.startImport(file);
            return Result.success("导入任务已启动", taskId);
        } catch (Exception e) {
            return Result.error("启动导入失败：" + e.getMessage());
        }
    }

    @PostMapping("/api/import/yukou-qkg")
    @ResponseBody
    public Result<Integer> importYukouQkg(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return Result.error("请选择要导入的文件");
            }

            String fileName = file.getOriginalFilename();
            if (fileName == null || (!fileName.endsWith(".xlsx") && !fileName.endsWith(".xls"))) {
                return Result.error("请选择Excel文件（.xlsx或.xls格式）");
            }

            int count = yukouQkgImportService.importQkg(file);
            return Result.success("导入成功", count);
        } catch (Exception e) {
            return Result.error("导入失败：" + e.getMessage());
        }
    }

    @PostMapping("/api/import/yukou-jl")
    @ResponseBody
    public Result<Integer> importYukouJl(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return Result.error("请选择要导入的文件");
            }

            String fileName = file.getOriginalFilename();
            if (fileName == null || !fileName.endsWith(".xlsx")) {
                return Result.error("请选择Excel文件（.xlsx格式）");
            }

            int count = yukouJlImportService.importJl(file);
            return Result.success("导入成功", count);
        } catch (Exception e) {
            return Result.error("导入失败：" + e.getMessage());
        }
    }

    @PostMapping("/api/import/yijiao")
    @ResponseBody
    public Result<String> importYijiao(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return Result.error("请选择要导入的文件");
            }

            String fileName = file.getOriginalFilename();

            if (fileName == null || (!fileName.endsWith(".xlsx") && !fileName.endsWith(".xls"))) {
                return Result.error("请选择Excel文件（.xlsx或.xls格式）");
            }

            String taskId = yijiaoImportService.startImport(file);
            return Result.success("导入任务已启动", taskId);
        } catch (Exception e) {
            return Result.error("启动导入失败：" + e.getMessage());
        }
    }
    
    // ========== 退税着急名单 ==========

    @PostMapping("/api/import/tuishui")
    @ResponseBody
    public Result<Integer> importTuishui(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return Result.error("请选择要导入的文件");
            }

            String fileName = file.getOriginalFilename();
            if (fileName == null || (!fileName.endsWith(".xlsx") && !fileName.endsWith(".xls"))) {
                return Result.error("请选择Excel文件（.xlsx或.xls格式）");
            }

            int count = taxTuishuiJImportService.importTuishuiData(file);
            return Result.success("导入成功", count);
        } catch (Exception e) {
            return Result.error("导入失败：" + e.getMessage());
        }
    }

    @PostMapping("/api/tuishui/calculate")
    @ResponseBody
    public Result<Integer> calculateTuishuiInfo() {
        try {
            int count = taxTuishuiJService.calculateTuishuiInfo();
            return Result.success("计算成功", count);
        } catch (Exception e) {
            return Result.error("计算失败：" + e.getMessage());
        }
    }

    @PostMapping("/api/tuishui/query")
    @ResponseBody
    public Result<PageResult<TaxTuishuiJ>> queryTuishuiList(@RequestBody(required = false) TaxQueryRequest request) {
        try {
            return Result.success(taxTuishuiJService.queryTuishuiList(request));
        } catch (Exception e) {
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    @PostMapping("/api/tuishui/export")
    public void exportTuishuiList(@RequestParam(value = "userName", required = false) String userName,
                                   @RequestParam(value = "idCard", required = false) String idCard,
                                   HttpServletResponse response) {
        Workbook workbook = new XSSFWorkbook();
        try {
            TaxQueryRequest request = new TaxQueryRequest();
            request.setUserName(userName);
            request.setIdCard(idCard);
            List<TaxTuishuiJ> list = taxTuishuiJService.queryAllTuishuiList(request);
            Sheet sheet = workbook.createSheet("退税着急名单");

            String[] headers = {"姓名", "身份证号", "联系电话", "退税金额", "预扣金额", "实缴金额", "（预扣-实缴）金额", "涉及税地", "涉及商户", "涉及渠道", "涉及销售", "涉及客服"};
            Row headerRow = sheet.createRow(0);
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            for (int i = 0; i < list.size(); i++) {
                TaxTuishuiJ item = list.get(i);
                Row row = sheet.createRow(i + 1);
                row.createCell(0).setCellValue(nullToEmpty(item.getUserName()));
                row.createCell(1).setCellValue(nullToEmpty(item.getIdCard()));
                row.createCell(2).setCellValue(nullToEmpty(item.getPhone()));
                row.createCell(3).setCellValue(formatNumber(item.getTsAmount()));
                row.createCell(4).setCellValue(formatNumber(item.getPreDeduct()));
                row.createCell(5).setCellValue(formatNumber(item.getActualPay()));
                row.createCell(6).setCellValue(formatNumber(item.getDiffAmount()));
                row.createCell(7).setCellValue(nullToEmpty(item.getTaxArea()));
                row.createCell(8).setCellValue(nullToEmpty(item.getMerchant()));
                row.createCell(9).setCellValue(nullToEmpty(item.getChannel()));
                row.createCell(10).setCellValue(nullToEmpty(item.getSale()));
                row.createCell(11).setCellValue(nullToEmpty(item.getCustomerService()));
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            String fileName = URLEncoder.encode("退税着急名单", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + fileName + ".xlsx");

            try (OutputStream os = response.getOutputStream()) {
                workbook.write(os);
                os.flush();
            }
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            try {
                workbook.close();
            } catch (Exception ignored) {
            }
        }
    }

    // ========== 退税全量名单 ==========

    @PostMapping("/api/tuishui-all/calculate")
    @ResponseBody
    public Result<String> calculateTuishuiAll() {
        try {
            String taskId = taxTuishuiAllService.startCalculate();
            return Result.success("计算任务已启动", taskId);
        } catch (Exception e) {
            return Result.error("启动计算失败：" + e.getMessage());
        }
    }

    @PostMapping("/api/tuishui-all/query")
    @ResponseBody
    public Result<PageResult<TaxTuishuiAll>> queryTuishuiAllList(@RequestBody(required = false) TaxQueryRequest request) {
        try {
            return Result.success(taxTuishuiAllService.queryAllList(request));
        } catch (Exception e) {
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    @PostMapping("/api/tuishui-all/export")
    public void exportTuishuiAllList(@RequestParam(value = "userName", required = false) String userName,
                                      @RequestParam(value = "idCard", required = false) String idCard,
                                      HttpServletResponse response) {
        Workbook workbook = new XSSFWorkbook();
        try {
            TaxQueryRequest request = new TaxQueryRequest();
            request.setUserName(userName);
            request.setIdCard(idCard);
            List<TaxTuishuiAll> list = taxTuishuiAllService.queryAllListForExport(request);
            Sheet sheet = workbook.createSheet("退税全量名单");

            String[] headers = {"姓名", "身份证号", "联系电话", "预扣金额", "实缴金额", "（预扣-实缴）金额", "涉及税地", "涉及商户", "涉及渠道", "涉及销售", "涉及客服"};
            Row headerRow = sheet.createRow(0);
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            for (int i = 0; i < list.size(); i++) {
                TaxTuishuiAll item = list.get(i);
                Row row = sheet.createRow(i + 1);
                row.createCell(0).setCellValue(nullToEmpty(item.getUserName()));
                row.createCell(1).setCellValue(nullToEmpty(item.getIdCard()));
                row.createCell(2).setCellValue(nullToEmpty(item.getPhone()));
                row.createCell(3).setCellValue(formatNumber(item.getPreDeduct()));
                row.createCell(4).setCellValue(formatNumber(item.getActualPay()));
                row.createCell(5).setCellValue(formatNumber(item.getDiffAmount()));
                row.createCell(6).setCellValue(nullToEmpty(item.getTaxArea()));
                row.createCell(7).setCellValue(nullToEmpty(item.getMerchant()));
                row.createCell(8).setCellValue(nullToEmpty(item.getChannel()));
                row.createCell(9).setCellValue(nullToEmpty(item.getSale()));
                row.createCell(10).setCellValue(nullToEmpty(item.getCustomerService()));
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            String fileName = URLEncoder.encode("退税全量名单", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + fileName + ".xlsx");

            try (OutputStream os = response.getOutputStream()) {
                workbook.write(os);
                os.flush();
            }
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            try {
                workbook.close();
            } catch (Exception ignored) {
            }
        }
    }

    @GetMapping("/api/tuishui-all/progress/{taskId}")
    @ResponseBody
    public Result<ImportProgress> getTuishuiAllProgress(@PathVariable String taskId) {
        ImportProgress progress = taxTuishuiAllService.getProgress(taskId);
        if (progress == null) {
            return Result.error("任务不存在");
        }
        return Result.success(progress);
    }

    @GetMapping("/api/import/progress/{taskId}")
    @ResponseBody
    public Result<ImportProgress> getProgress(@PathVariable String taskId) {
        ImportProgress progress = yukouImportService.getProgress(taskId);
        if (progress == null) {
            progress = yijiaoImportService.getProgress(taskId);
        }
        if (progress == null) {
            progress = taxTuishuiAllService.getProgress(taskId);
        }
        if (progress == null) {
            return Result.error("任务不存在");
        }
        return Result.success(progress);
    }
}
