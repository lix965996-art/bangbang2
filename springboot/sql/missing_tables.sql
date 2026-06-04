-- =====================================================
-- 补充缺失的建表脚本（11张表）
-- 对应 Entity: FarmlandAlert, SensorReading, CropYieldConfig,
--   HealthIndexConfig, InventoryOutbound, OnlineSale,
--   MiniUser, Reservation, GroupBuy, GroupBuyOrder, Badge
-- =====================================================

-- 1. 农田预警表
CREATE TABLE IF NOT EXISTS `farmland_alert` (
  `id` int NOT NULL AUTO_INCREMENT,
  `farmland_id` int DEFAULT NULL COMMENT '农田ID',
  `farmland_name` varchar(100) DEFAULT NULL COMMENT '农田名称',
  `alert_type` varchar(50) DEFAULT NULL COMMENT '预警类型: temperature/soil_humidity/air_humidity/ph/carbon/light',
  `alert_level` varchar(20) DEFAULT NULL COMMENT '预警等级: low/medium/high',
  `current_value` decimal(10,2) DEFAULT NULL COMMENT '当前值',
  `threshold_min` decimal(10,2) DEFAULT NULL COMMENT '阈值下限',
  `threshold_max` decimal(10,2) DEFAULT NULL COMMENT '阈值上限',
  `message` varchar(500) DEFAULT NULL COMMENT '预警消息',
  `suggestion` varchar(500) DEFAULT NULL COMMENT '操作建议',
  `status` varchar(20) DEFAULT 'pending' COMMENT '状态: pending/processed',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `process_time` datetime DEFAULT NULL COMMENT '处理时间',
  `processor` varchar(100) DEFAULT NULL COMMENT '处理人',
  PRIMARY KEY (`id`),
  KEY `idx_farmland_id` (`farmland_id`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='农田预警表';

-- 2. 传感器读数表
CREATE TABLE IF NOT EXISTS `sensor_reading` (
  `id` int NOT NULL AUTO_INCREMENT,
  `temperature` double DEFAULT NULL COMMENT '温度(℃)',
  `humidity` double DEFAULT NULL COMMENT '湿度(%)',
  `led` int DEFAULT 0 COMMENT 'LED状态: 0=关, 1=开',
  `device_name` varchar(100) DEFAULT NULL COMMENT '设备名称',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_device_name` (`device_name`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='传感器读数表';

-- 3. 作物产量配置表
CREATE TABLE IF NOT EXISTS `crop_yield_config` (
  `id` int NOT NULL AUTO_INCREMENT,
  `crop_name` varchar(50) NOT NULL COMMENT '作物名称',
  `yield_per_mu` decimal(10,2) NOT NULL DEFAULT 0 COMMENT '每亩产量(公斤)',
  `unit_price` decimal(10,2) NOT NULL DEFAULT 0 COMMENT '销售单价(元/公斤)',
  `cost_per_mu` decimal(10,2) NOT NULL DEFAULT 0 COMMENT '每亩成本(元)',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_crop_name` (`crop_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='作物产量配置表';

-- 4. 健康指数配置表
CREATE TABLE IF NOT EXISTS `health_index_config` (
  `id` int NOT NULL AUTO_INCREMENT,
  `indicator` varchar(50) NOT NULL COMMENT '指标名称: temperature/air_humidity/soil_humidity/carbon/ph/light',
  `excellent_min` decimal(10,2) DEFAULT NULL COMMENT '优秀区间最小值',
  `excellent_max` decimal(10,2) DEFAULT NULL COMMENT '优秀区间最大值',
  `good_min` decimal(10,2) DEFAULT NULL COMMENT '良好区间最小值',
  `good_max` decimal(10,2) DEFAULT NULL COMMENT '良好区间最大值',
  `weight` decimal(4,2) DEFAULT NULL COMMENT '权重(0-1)',
  `threshold_min` decimal(10,2) DEFAULT NULL COMMENT '预警下限',
  `threshold_max` decimal(10,2) DEFAULT NULL COMMENT '预警上限',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_indicator` (`indicator`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='健康指数配置表';

-- 5. 物资出库记录表
CREATE TABLE IF NOT EXISTS `inventory_outbound` (
  `id` int NOT NULL AUTO_INCREMENT,
  `inventory_id` int DEFAULT NULL COMMENT '物资ID',
  `produce` varchar(100) DEFAULT NULL COMMENT '产品名称',
  `quantity` int DEFAULT NULL COMMENT '出库数量',
  `outbound_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '出库时间',
  `operator` varchar(100) DEFAULT NULL COMMENT '操作人',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_inventory_id` (`inventory_id`),
  KEY `idx_outbound_time` (`outbound_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='物资出库记录表';

-- 6. 农作物在线销售表
CREATE TABLE IF NOT EXISTS `online_sale` (
  `id` int NOT NULL AUTO_INCREMENT,
  `inventory_id` int DEFAULT NULL COMMENT '关联库存ID',
  `produce` varchar(100) DEFAULT NULL COMMENT '商品名称',
  `warehouse` varchar(100) DEFAULT NULL COMMENT '所属仓库',
  `quantity` int DEFAULT NULL COMMENT '出售数量',
  `price` decimal(10,2) DEFAULT NULL COMMENT '单价(元)',
  `total_price` decimal(10,2) DEFAULT NULL COMMENT '总价(元)',
  `status` varchar(20) DEFAULT NULL COMMENT '状态',
  `seller` varchar(100) DEFAULT NULL COMMENT '销售员',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_inventory_id` (`inventory_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='农作物在线销售表';

-- 7. 小程序用户表
CREATE TABLE IF NOT EXISTS `mini_user` (
  `id` int NOT NULL AUTO_INCREMENT,
  `openid` varchar(100) NOT NULL COMMENT '微信openid',
  `nickname` varchar(100) DEFAULT NULL COMMENT '昵称',
  `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
  `avatar_url` varchar(500) DEFAULT NULL COMMENT '头像URL',
  `role` varchar(20) DEFAULT 'visitor' COMMENT '角色: visitor/farmer',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_openid` (`openid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='小程序用户表';

-- 8. 预约记录表
CREATE TABLE IF NOT EXISTS `reservation` (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id` int DEFAULT NULL COMMENT '用户ID',
  `farm_id` int DEFAULT NULL COMMENT '农场ID',
  `visit_date` date DEFAULT NULL COMMENT '到访日期',
  `visit_time` varchar(20) DEFAULT NULL COMMENT '到访时间',
  `car_number` varchar(20) DEFAULT NULL COMMENT '车牌号',
  `expected_weight` decimal(10,2) DEFAULT NULL COMMENT '预计重量(kg)',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `status` varchar(20) DEFAULT 'pending' COMMENT '状态: pending/confirmed/cancelled/completed',
  `verify_code` varchar(50) DEFAULT NULL COMMENT '核销码',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='预约记录表';

-- 9. 团购表
CREATE TABLE IF NOT EXISTS `group_buy` (
  `id` int NOT NULL AUTO_INCREMENT,
  `farm_id` int DEFAULT NULL COMMENT '农场ID',
  `creator_id` int DEFAULT NULL COMMENT '发起人ID',
  `product_name` varchar(100) DEFAULT NULL COMMENT '产品名称',
  `price` decimal(10,2) DEFAULT NULL COMMENT '团购价(元)',
  `original_price` decimal(10,2) DEFAULT NULL COMMENT '原价(元)',
  `target_weight` decimal(10,2) DEFAULT NULL COMMENT '目标重量(kg)',
  `current_weight` decimal(10,2) DEFAULT 0 COMMENT '当前已凑重量(kg)',
  `status` varchar(20) DEFAULT 'active' COMMENT '状态: active/completed/cancelled',
  `description` text COMMENT '描述',
  `end_time` datetime DEFAULT NULL COMMENT '截止时间',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='团购表';

-- 10. 团购参与记录表
CREATE TABLE IF NOT EXISTS `group_buy_order` (
  `id` int NOT NULL AUTO_INCREMENT,
  `group_buy_id` int DEFAULT NULL COMMENT '团购ID',
  `user_id` int DEFAULT NULL COMMENT '用户ID',
  `weight` decimal(10,2) DEFAULT NULL COMMENT '参与重量(kg)',
  `amount` decimal(10,2) DEFAULT NULL COMMENT '金额(元)',
  `status` varchar(20) DEFAULT 'paid' COMMENT '状态: paid/cancelled',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_group_buy_id` (`group_buy_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='团购参与记录表';

-- 11. 成就徽章表
CREATE TABLE IF NOT EXISTS `badge` (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id` int DEFAULT NULL COMMENT '用户ID',
  `badge_type` varchar(50) DEFAULT NULL COMMENT '徽章类型',
  `progress` int DEFAULT 0 COMMENT '当前进度',
  `target` int DEFAULT 0 COMMENT '目标值',
  `unlocked` int DEFAULT 0 COMMENT '是否解锁: 0=否, 1=是',
  `unlocked_at` datetime DEFAULT NULL COMMENT '解锁时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_badge` (`user_id`, `badge_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='成就徽章表';
