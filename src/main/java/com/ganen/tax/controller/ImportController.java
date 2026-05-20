package com.ganen.tax.controller;

import com.ganen.tax.common.Result;
import com.ganen.tax.dto.ImportProgress;
import com.ganen.tax.service.ExcelImportService;
import com.ganen.tax.service.YukouImportService;
import com.ganen.tax.service.YukouJlImportService;
import com.ganen.tax.service.YukouQkgImportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class ImportController {
    
    @Autowired
    private ExcelImportService excelImportService;
    
    @Autowired
    private YukouImportService yukouImportService;
    
    @Autowired
    private YukouQkgImportService yukouQkgImportService;

    @Autowired
    private YukouJlImportService yukouJlImportService;
    
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
    
    @PostMapping("/api/import/yukou")
    @ResponseBody
    public Result<String> importYukou(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return Result.error("请选择要导入的文件");
            }
            
            String fileName = file.getOriginalFilename();
            if (fileName == null || !fileName.endsWith(".xlsx")) {
                return Result.error("请选择Excel文件（.xlsx格式）");
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
            if (fileName == null || !fileName.endsWith(".xlsx")) {
                return Result.error("请选择Excel文件（.xlsx格式）");
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
    
    @GetMapping("/api/import/progress/{taskId}")
    @ResponseBody
    public Result<ImportProgress> getProgress(@PathVariable String taskId) {
        ImportProgress progress = yukouImportService.getProgress(taskId);
        if (progress == null) {
            return Result.error("任务不存在");
        }
        return Result.success(progress);
    }
}
