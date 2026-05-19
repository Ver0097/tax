package com.ganen.tax.controller;

import com.ganen.tax.common.Result;
import com.ganen.tax.service.ExcelImportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class ImportController {
    
    @Autowired
    private ExcelImportService excelImportService;
    
    @GetMapping("/")
    public String index() {
        return "index";
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
}