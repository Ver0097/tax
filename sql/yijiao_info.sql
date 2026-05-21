CREATE TABLE `yijiao_info` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `paid_amount` decimal(12,2) NOT NULL DEFAULT 0.00 COMMENT '已缴金额',
  `user_name` varchar(30) NOT NULL DEFAULT '' COMMENT '姓名',
  `id_card` varchar(18) NOT NULL DEFAULT '' COMMENT '身份证号码',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_id_card` (`id_card`) COMMENT '身份证查询索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='已缴税费信息表';