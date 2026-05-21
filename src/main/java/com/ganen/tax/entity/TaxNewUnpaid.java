package com.ganen.tax.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("tax")
public class TaxNewUnpaid {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String userName;

    private String idCard;

    private String phone;

    private String withholdingAgent;

    private BigDecimal shouldPay;

    private BigDecimal preDeduct;

    private BigDecimal actualPay;

    private BigDecimal diffAmount;

    private BigDecimal recoverPay;

    private String taxArea;

    private String merchant;

    private String channel;

    private String sale;

    private String customerService;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
