package com.ganen.tax.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ganen.tax.entity.TaxTuishuiAllQfsd;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 退税全量名单（区分税地） Mapper
 */
@Mapper
public interface TaxTuishuiAllQfsdMapper extends BaseMapper<TaxTuishuiAllQfsd> {

    /** 清空表 */
    void truncateTable();

    /** 分页获取 v_yukou_all 中去重后的身份证号 */
    List<String> selectDistinctIdCards(@Param("offset") long offset, @Param("limit") int limit);

    /** 根据一批身份证号，按 id_card + tax_area 分组计算并插入 */
    int insertBatchCompute(@Param("idCards") List<String> idCards);

    /** 补入孤儿实缴：只处理指定id_card中税地不匹配的记录 */
    int insertOrphanActualPay(@Param("orphanIdCards") List<String> orphanIdCards);

    /** 统计数量 */
    long countAllList(@Param("userName") String userName, @Param("idCard") String idCard);

    /** 分页查询 */
    List<TaxTuishuiAllQfsd> queryAllList(@Param("userName") String userName,
                                          @Param("idCard") String idCard,
                                          @Param("offset") long offset,
                                          @Param("pageSize") long pageSize);
}
