CREATE TABLE `tax_unpaid` (
    `id` int NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_name` varchar(30) NOT NULL DEFAULT '' COMMENT '姓名',
    `id_card` varchar(18) NOT NULL COMMENT '身份证号',
    `phone` varchar(11) NOT NULL DEFAULT '' COMMENT '联系电话',
    `should_pay` decimal(12,2) NOT NULL DEFAULT '0.00' COMMENT '应补金额',
    `pre_deduct` decimal(12,2) NOT NULL DEFAULT '0.00' COMMENT '预扣金额',
    `actual_pay` decimal(12,2) NOT NULL DEFAULT '0.00' COMMENT '实缴金额',
    `recover_pay` decimal(12,2) NOT NULL DEFAULT '0.00' COMMENT '追缴金额',
    `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态:0未补缴,1部分补缴,2已结清,3作废',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_id_card` (`id_card`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='未补税人员信息统计表';