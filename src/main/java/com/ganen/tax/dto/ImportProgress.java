package com.ganen.tax.dto;

import lombok.Data;

@Data
public class ImportProgress {
    private String taskId;
    private int totalRows;
    private int processedRows;
    private int successCount;
    private int failCount;
    private String status;
    private String message;
    private long startTime;
    private long endTime;
    
    public static ImportProgress create(String taskId) {
        ImportProgress progress = new ImportProgress();
        progress.setTaskId(taskId);
        progress.setTotalRows(0);
        progress.setProcessedRows(0);
        progress.setSuccessCount(0);
        progress.setFailCount(0);
        progress.setStatus("processing");
        progress.setMessage("正在处理...");
        progress.setStartTime(System.currentTimeMillis());
        return progress;
    }
    
    public void update(int processed, int success, int fail) {
        this.processedRows = processed;
        this.successCount = success;
        this.failCount = fail;
        double percent = totalRows > 0 ? (processed * 100.0 / totalRows) : 0;
        this.message = String.format("处理进度: %.1f%% (%d/%d)", percent, processed, totalRows);
    }
    
    public void complete(int success, int fail) {
        this.processedRows = totalRows;
        this.successCount = success;
        this.failCount = fail;
        this.status = "completed";
        this.endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        this.message = String.format("导入完成！成功%d条，失败%d条，耗时%.1f秒", success, fail, duration / 1000.0);
    }
    
    public void error(String errorMsg) {
        this.status = "error";
        this.endTime = System.currentTimeMillis();
        this.message = "导入失败: " + errorMsg;
    }
}