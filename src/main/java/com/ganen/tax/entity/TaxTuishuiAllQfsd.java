package com.ganen.tax.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 退税全量名单（区分税地）实体
 * 同一个人按税地拆分为多条记录
 */
@Data
@TableName("tax_tuishui_all_qfsd")
public class TaxTuishuiAllQfsd {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 姓名 */
    private String userName;

    /** 身份证号 */
    private String idCard;

    /** 联系电话 */
    private String phone;

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

    /** 状态 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
