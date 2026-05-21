package com.ganen.tax.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ganen.tax.entity.Tax;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TaxMapper extends BaseMapper<Tax> {

    @Update("""
            UPDATE tax t
            LEFT JOIN (
                SELECT
                    id_card,
                    COALESCE(SUM(tax_amount), 0) AS total_tax_amount,
                    GROUP_CONCAT(DISTINCT NULLIF(tax_area, '') ORDER BY tax_area SEPARATOR ',') AS tax_area,
                    GROUP_CONCAT(DISTINCT NULLIF(merchant, '') ORDER BY merchant SEPARATOR ',') AS merchant,
                    GROUP_CONCAT(DISTINCT NULLIF(channel, '') ORDER BY channel SEPARATOR ',') AS channel,
                    GROUP_CONCAT(DISTINCT NULLIF(sale, '') ORDER BY sale SEPARATOR ',') AS sale,
                    GROUP_CONCAT(DISTINCT NULLIF(customer_service, '') ORDER BY customer_service SEPARATOR ',') AS customer_service
                FROM v_yukou_all
                GROUP BY id_card
            ) vy ON t.id_card = vy.id_card
            LEFT JOIN (
                SELECT id_card, COALESCE(SUM(paid_amount), 0) AS total_paid_amount
                FROM yijiao_info
                GROUP BY id_card
            ) yi ON t.id_card = yi.id_card
            SET
                t.pre_deduct = COALESCE(vy.total_tax_amount, 0),
                t.actual_pay = COALESCE(yi.total_paid_amount, 0),
                t.diff_amount = COALESCE(vy.total_tax_amount, 0) - COALESCE(yi.total_paid_amount, 0),
                t.recover_pay = COALESCE(t.should_pay, 0) - (COALESCE(vy.total_tax_amount, 0) - COALESCE(yi.total_paid_amount, 0)),
                t.tax_area = COALESCE(vy.tax_area, ''),
                t.merchant = COALESCE(vy.merchant, ''),
                t.channel = COALESCE(vy.channel, ''),
                t.sale = COALESCE(vy.sale, ''),
                t.customer_service = COALESCE(vy.customer_service, ''),
                t.update_time = NOW()
            """)
    int calculateRecoverInfo();

    @Select("SELECT COUNT(*) FROM tax")
    long countAll();

    List<Tax> queryTaxList(@Param("userName") String userName, @Param("idCard") String idCard);
}
