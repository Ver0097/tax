CREATE TABLE `yukou_qkg_info` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tax_amount` decimal(12,2) NOT NULL DEFAULT '0.00' COMMENT '个税金额',
    `tax_area` varchar(50) NOT NULL DEFAULT '' COMMENT '税地',
    `payee` varchar(30) NOT NULL DEFAULT '' COMMENT '收款人',
    `id_card` varchar(18) NOT NULL DEFAULT '' COMMENT '身份证号',
    `phone` varchar(11) NOT NULL DEFAULT '' COMMENT '联系方式',
    `merchant` varchar(50) NOT NULL DEFAULT '' COMMENT '商户',
    `channel` varchar(30) NOT NULL DEFAULT '' COMMENT '渠道',
    `sale` varchar(30) NOT NULL DEFAULT '' COMMENT '销售',
    `customer_service` varchar(30) NOT NULL DEFAULT '' COMMENT '客服',
    `order_source` tinyint NOT NULL DEFAULT '0' COMMENT '订单来源：0-平台，1-趣开工，2-京灵',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_id_card` (`id_card`) COMMENT '身份证索引',
    KEY `idx_order_source` (`order_source`) COMMENT '订单来源索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='趣开工预扣信息表';
