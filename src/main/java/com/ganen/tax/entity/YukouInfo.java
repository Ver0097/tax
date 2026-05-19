package com.ganen.tax.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("yukou_info")
public class YukouInfo {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private BigDecimal taxAmount;
    
    private String taxArea;
    
    private String payee;
    
    private String idCard;
    
    private String phone;
    
    private String merchant;
    
    private String channel;
    
    private String sale;
    
    private String customerService;
    
    private Integer orderSource;
    
    private LocalDateTime createTime;
    
    private LocalDateTime updateTime;
}