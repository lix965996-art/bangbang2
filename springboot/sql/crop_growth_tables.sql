-- 作物生长阶段配置表
CREATE TABLE IF NOT EXISTS crop_growth_profile (
    id INT AUTO_INCREMENT PRIMARY KEY,
    crop_name VARCHAR(50) NOT NULL COMMENT '作物名称',
    stage VARCHAR(50) NOT NULL COMMENT '阶段标识: seeding/seedling/vegetative/flowering/fruiting/harvest/dormant',
    stage_name VARCHAR(50) NOT NULL COMMENT '阶段中文名: 播种期/苗期/营养生长期/花期/果期/采收期/休眠期',

    -- 积温参数
    base_temp DECIMAL(5,2) DEFAULT 10.00 COMMENT '基础温度(℃)，低于此不累计积温',
    gdd_target DECIMAL(7,1) NOT NULL COMMENT '本阶段目标积温(GDD)，达到后进入下一阶段',

    -- 理想环境范围
    temp_min DECIMAL(5,2) COMMENT '最低温度(℃)',
    temp_max DECIMAL(5,2) COMMENT '最高温度(℃)',
    humidity_min INT COMMENT '最低空气湿度(%)',
    humidity_max INT COMMENT '最高空气湿度(%)',
    light_min INT COMMENT '最低光照(lux)',
    light_max INT COMMENT '最高光照(lux)',
    soil_moisture_min INT COMMENT '最低土壤湿度(%)',
    soil_moisture_max INT COMMENT '最高土壤湿度(%)',

    -- 自动化参数倍率
    water_factor DECIMAL(3,2) DEFAULT 1.00 COMMENT '灌溉量倍率(1.0=标准)',
    light_factor DECIMAL(3,2) DEFAULT 1.00 COMMENT '补光强度倍率',

    duration_days INT COMMENT '预计持续天数(参考值)',
    sort_order INT DEFAULT 0 COMMENT '阶段排序(从0开始)',

    UNIQUE KEY uk_crop_stage (crop_name, stage)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='作物生长阶段配置表';

-- 农场作物生长追踪表
CREATE TABLE IF NOT EXISTS farm_crop_tracker (
    id INT AUTO_INCREMENT PRIMARY KEY,
    farm_name VARCHAR(100) NOT NULL COMMENT '农场名称',
    crop_name VARCHAR(50) NOT NULL COMMENT '作物名称',
    current_stage VARCHAR(50) NOT NULL DEFAULT 'seeding' COMMENT '当前阶段',
    planting_date DATE NOT NULL COMMENT '播种日期',
    accumulated_gdd DECIMAL(7,1) DEFAULT 0.0 COMMENT '累计积温(GDD)',
    last_gdd_update DATE COMMENT '上次积温更新日期',
    expected_harvest DATE COMMENT '预计采收日期',
    stage_updated_at DATETIME COMMENT '阶段更新时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uk_farm_crop (farm_name, crop_name),
    INDEX idx_stage (current_stage)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='农场作物生长追踪表';

-- 预置作物生长阶段数据

-- ========== 番茄 ==========
INSERT INTO crop_growth_profile (crop_name, stage, stage_name, base_temp, gdd_target, temp_min, temp_max, humidity_min, humidity_max, light_min, light_max, soil_moisture_min, soil_moisture_max, water_factor, light_factor, duration_days, sort_order) VALUES
('番茄', 'seeding', '播种期', 10.0, 50.0, 18, 28, 60, 80, NULL, NULL, 60, 80, 0.6, 0.5, 7, 0),
('番茄', 'seedling', '苗期', 10.0, 200.0, 15, 25, 50, 70, 15000, 30000, 50, 70, 0.7, 0.6, 25, 1),
('番茄', 'vegetative', '营养生长期', 10.0, 400.0, 18, 28, 50, 70, 20000, 40000, 55, 75, 0.9, 0.8, 30, 2),
('番茄', 'flowering', '花期', 12.0, 300.0, 20, 30, 55, 75, 25000, 50000, 55, 70, 1.0, 1.0, 20, 3),
('番茄', 'fruiting', '果期', 12.0, 600.0, 20, 30, 55, 75, 25000, 50000, 60, 80, 1.3, 1.0, 40, 4),
('番茄', 'harvest', '采收期', 10.0, 200.0, 18, 28, 50, 70, 20000, 40000, 50, 65, 0.8, 0.5, 20, 5);

-- ========== 玉米 ==========
INSERT INTO crop_growth_profile (crop_name, stage, stage_name, base_temp, gdd_target, temp_min, temp_max, humidity_min, humidity_max, light_min, light_max, soil_moisture_min, soil_moisture_max, water_factor, light_factor, duration_days, sort_order) VALUES
('玉米', 'seeding', '播种期', 10.0, 80.0, 12, 25, NULL, NULL, NULL, NULL, 60, 80, 0.5, 0.3, 10, 0),
('玉米', 'seedling', '苗期', 10.0, 250.0, 15, 28, 50, 70, 15000, 35000, 50, 70, 0.7, 0.6, 25, 1),
('玉米', 'vegetative', '营养生长期', 10.0, 500.0, 18, 32, 50, 75, 25000, 50000, 55, 80, 1.1, 0.9, 35, 2),
('玉米', 'flowering', '花期', 12.0, 250.0, 22, 32, 55, 80, 30000, 55000, 60, 80, 1.3, 1.0, 15, 3),
('玉米', 'fruiting', '果期', 10.0, 400.0, 18, 30, 50, 70, 25000, 50000, 55, 75, 1.0, 0.8, 30, 4),
('玉米', 'harvest', '采收期', 8.0, 150.0, 15, 28, 45, 65, 20000, 40000, 45, 60, 0.5, 0.3, 15, 5);

-- ========== 小麦 ==========
INSERT INTO crop_growth_profile (crop_name, stage, stage_name, base_temp, gdd_target, temp_min, temp_max, humidity_min, humidity_max, light_min, light_max, soil_moisture_min, soil_moisture_max, water_factor, light_factor, duration_days, sort_order) VALUES
('小麦', 'seeding', '播种期', 0.0, 100.0, 5, 18, NULL, NULL, NULL, NULL, 60, 80, 0.5, 0.3, 15, 0),
('小麦', 'seedling', '苗期', 0.0, 300.0, 3, 15, 50, 70, 10000, 25000, 45, 65, 0.6, 0.5, 30, 1),
('小麦', 'vegetative', '营养生长期', 0.0, 400.0, 5, 20, 45, 70, 15000, 35000, 50, 70, 0.8, 0.7, 40, 2),
('小麦', 'flowering', '花期', 5.0, 250.0, 12, 25, 50, 75, 20000, 45000, 50, 70, 1.0, 0.9, 20, 3),
('小麦', 'fruiting', '灌浆期', 5.0, 450.0, 15, 28, 45, 65, 20000, 45000, 45, 65, 0.9, 0.8, 30, 4),
('小麦', 'harvest', '采收期', 5.0, 150.0, 15, 30, 40, 60, 15000, 35000, 35, 50, 0.3, 0.2, 10, 5);

-- ========== 艾叶 ==========
INSERT INTO crop_growth_profile (crop_name, stage, stage_name, base_temp, gdd_target, temp_min, temp_max, humidity_min, humidity_max, light_min, light_max, soil_moisture_min, soil_moisture_max, water_factor, light_factor, duration_days, sort_order) VALUES
('艾叶', 'seeding', '播种期', 8.0, 60.0, 12, 22, 60, 80, NULL, NULL, 55, 75, 0.5, 0.3, 10, 0),
('艾叶', 'seedling', '苗期', 8.0, 200.0, 12, 25, 50, 70, 10000, 25000, 45, 65, 0.6, 0.5, 20, 1),
('艾叶', 'vegetative', '营养生长期', 8.0, 350.0, 15, 28, 45, 70, 15000, 35000, 50, 70, 0.8, 0.7, 30, 2),
('艾叶', 'harvest', '采收期', 8.0, 200.0, 15, 30, 40, 65, 15000, 35000, 40, 60, 0.5, 0.3, 15, 3);

-- ========== 竹子 ==========
INSERT INTO crop_growth_profile (crop_name, stage, stage_name, base_temp, gdd_target, temp_min, temp_max, humidity_min, humidity_max, light_min, light_max, soil_moisture_min, soil_moisture_max, water_factor, light_factor, duration_days, sort_order) VALUES
('竹子', 'seeding', '萌芽期', 8.0, 100.0, 10, 22, 60, 85, 5000, 20000, 60, 85, 0.8, 0.4, 15, 0),
('竹子', 'seedling', '出笋期', 8.0, 300.0, 12, 25, 60, 85, 10000, 30000, 60, 80, 1.0, 0.5, 25, 1),
('竹子', 'vegetative', '成竹期', 8.0, 600.0, 10, 30, 50, 80, 15000, 40000, 55, 75, 0.9, 0.7, 60, 2),
('竹子', 'harvest', '采伐期', 5.0, 200.0, 5, 25, 45, 70, 10000, 30000, 45, 65, 0.5, 0.3, 30, 3);

-- ========== 棕树 ==========
INSERT INTO crop_growth_profile (crop_name, stage, stage_name, base_temp, gdd_target, temp_min, temp_max, humidity_min, humidity_max, light_min, light_max, soil_moisture_min, soil_moisture_max, water_factor, light_factor, duration_days, sort_order) VALUES
('棕树', 'seedling', '苗期', 12.0, 400.0, 15, 28, 60, 80, 15000, 35000, 55, 75, 0.7, 0.5, 90, 0),
('棕树', 'vegetative', '生长期', 12.0, 800.0, 15, 32, 55, 80, 20000, 50000, 50, 75, 0.9, 0.8, 180, 1),
('棕树', 'harvest', '采收期', 10.0, 300.0, 12, 30, 50, 75, 20000, 45000, 45, 70, 0.6, 0.5, 60, 2);
