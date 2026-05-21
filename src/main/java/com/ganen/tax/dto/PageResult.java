package com.ganen.tax.dto;

import lombok.Data;

import java.util.List;

@Data
public class PageResult<T> {

    private long total;

    private long pageNo;

    private long pageSize;

    private List<T> records;

    public static <T> PageResult<T> of(long total, long pageNo, long pageSize, List<T> records) {
        PageResult<T> result = new PageResult<>();
        result.setTotal(total);
        result.setPageNo(pageNo);
        result.setPageSize(pageSize);
        result.setRecords(records);
        return result;
    }
}
