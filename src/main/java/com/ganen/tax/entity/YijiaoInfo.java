package com.ganen.tax.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("yijiao_info")
public class YijiaoInfo {

    @TableId(type = IdType.AUTO)
    private Long id;

    private BigDecimal paidAmount;

    private String userName;

    private String idCard;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
