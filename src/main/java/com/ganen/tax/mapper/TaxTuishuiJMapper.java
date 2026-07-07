package com.ganen.tax.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ganen.tax.entity.TaxTuishuiJ;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 退税着急名单 Mapper
 */
@Mapper
public interface TaxTuishuiJMapper extends BaseMapper<TaxTuishuiJ> {

    /**
     * 计算并更新退税着急名单的预扣金额、实缴金额、差额及相关信息
     */
    int calculateTuishuiInfo();

    /**
     * 统计退税着急名单数量
     */
    long countTuishuiList(@Param("userName") String userName, @Param("idCard") String idCard);

    /**
     * 分页查询退税着急名单
     */
    List<TaxTuishuiJ> queryTuishuiList(@Param("userName") String userName,
                                        @Param("idCard") String idCard,
                                        @Param("offset") long offset,
                                        @Param("pageSize") long pageSize);
}
