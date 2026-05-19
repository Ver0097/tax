package com.ganen.tax.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("tax_unpaid")
public class TaxUnpaid {
    
    @TableId(type = IdType.AUTO)
    private Integer id;
    
    private String userName;
    
    private String idCard;
    
    private String phone;
    
    private BigDecimal shouldPay;
    
    private BigDecimal preDeduct;
    
    private BigDecimal actualPay;
    
    private BigDecimal recoverPay;
    
    private Integer status;
    
    private LocalDateTime createTime;
    
    private LocalDateTime updateTime;
}