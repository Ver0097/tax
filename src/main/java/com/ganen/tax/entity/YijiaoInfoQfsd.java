package com.ganen.tax.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 已缴税费信息（区分税地）实体
 */
@Data
@TableName("yijiao_info_qfsd")
public class YijiaoInfoQfsd {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 已缴金额 */
    private BigDecimal paidAmount;

    /** 姓名 */
    private String userName;

    /** 身份证号码 */
    private String idCard;

    /** 涉及税地（从Excel第3行冒号后提取） */
    private String taxArea;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
