package com.ganen.tax.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 退税着急名单实体
 */
@Data
@TableName("tax_tuishui_j")
public class TaxTuishuiJ {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 姓名 */
    private String userName;

    /** 身份证号 */
    private String idCard;

    /** 联系电话（来源v_yukou_all，多笔只取一笔） */
    private String phone;

    /** 退税金额 */
    private BigDecimal tsAmount;

    /** 预扣金额 */
    private BigDecimal preDeduct;

    /** 实缴金额 */
    private BigDecimal actualPay;

    /** （预扣-实缴）金额 */
    private BigDecimal diffAmount;

    /** 涉及税地 */
    private String taxArea;

    /** 涉及商户 */
    private String merchant;

    /** 涉及渠道 */
    private String channel;

    /** 涉及销售 */
    private String sale;

    /** 涉及客服 */
    private String customerService;

    /** 状态:0未补缴,1部分补缴,2已结清,3作废 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
