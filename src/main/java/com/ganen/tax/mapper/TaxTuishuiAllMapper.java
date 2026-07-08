package com.ganen.tax.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ganen.tax.entity.TaxTuishuiAll;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 退税全量名单 Mapper
 */
@Mapper
public interface TaxTuishuiAllMapper extends BaseMapper<TaxTuishuiAll> {

    /** 清空表 */
    void truncateTable();

    /** 分页获取 v_yukou_all 中去重后的身份证号 */
    List<String> selectDistinctIdCards(@Param("offset") long offset, @Param("limit") int limit);

    /** 根据一批身份证号，计算并插入汇总数据 */
    int insertBatchCompute(@Param("idCards") List<String> idCards);

    /** 统计退税全量名单数量 */
    long countAllList(@Param("userName") String userName, @Param("idCard") String idCard);

    /** 分页查询退税全量名单 */
    List<TaxTuishuiAll> queryAllList(@Param("userName") String userName,
                                     @Param("idCard") String idCard,
                                     @Param("offset") long offset,
                                     @Param("pageSize") long pageSize);
}
