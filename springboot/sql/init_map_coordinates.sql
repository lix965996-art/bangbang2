-- =====================================================
-- 真实地图3D农场 - 坐标数据初始化（幂等执行）
-- =====================================================

-- 使用存储过程实现幂等的 ALTER TABLE（兼容 MySQL 8.0.19 以下版本）
DELIMITER //
CREATE PROCEDURE IF NOT EXISTS add_column_if_not_exists(
    IN p_table_name VARCHAR(64),
    IN p_column_name VARCHAR(64),
    IN p_column_def VARCHAR(500)
)
BEGIN
    DECLARE col_count INT DEFAULT 0;
    SELECT COUNT(*) INTO col_count
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = p_table_name
      AND COLUMN_NAME = p_column_name;
    IF col_count = 0 THEN
        SET @sql = CONCAT('ALTER TABLE `', p_table_name, '` ADD COLUMN ', p_column_def);
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END //
DELIMITER ;

CALL add_column_if_not_exists('statistic', 'center_lng', '`center_lng` DECIMAL(10, 7) NULL COMMENT ''中心经度'' AFTER `keeper`');
CALL add_column_if_not_exists('statistic', 'center_lat', '`center_lat` DECIMAL(10, 7) NULL COMMENT ''中心纬度'' AFTER `center_lng`');
CALL add_column_if_not_exists('statistic', 'coordinates', '`coordinates` TEXT NULL COMMENT ''区域坐标JSON'' AFTER `center_lat`');

-- 清理临时存储过程
DROP PROCEDURE IF EXISTS add_column_if_not_exists;

-- 为所有农田设置张家界地区的坐标
UPDATE `statistic` SET 
  `center_lng` = 110.476 + (id * 0.001),
  `center_lat` = 29.117 + (id * 0.001),
  `coordinates` = CONCAT(
    '[',
    '{"lng":', 110.476 + (id * 0.001), ',"lat":', 29.117 + (id * 0.001) + 0.0005, '},',
    '{"lng":', 110.476 + (id * 0.001) + 0.001, ',"lat":', 29.117 + (id * 0.001) + 0.0005, '},',
    '{"lng":', 110.476 + (id * 0.001) + 0.001, ',"lat":', 29.117 + (id * 0.001), '},',
    '{"lng":', 110.476 + (id * 0.001), ',"lat":', 29.117 + (id * 0.001), '}',
    ']'
  );

-- 查看结果
SELECT id, farm AS '农田', crop AS '作物', center_lng AS '经度', center_lat AS '纬度' FROM `statistic` ORDER BY id;
