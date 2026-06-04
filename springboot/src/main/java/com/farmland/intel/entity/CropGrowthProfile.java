package com.farmland.intel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 作物生长阶段配置
 */
@Data
@TableName("crop_growth_profile")
public class CropGrowthProfile implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /** 作物名称 */
    private String cropName;

    /** 阶段标识: seeding/seedling/vegetative/flowering/fruiting/harvest/dormant */
    private String stage;

    /** 阶段中文名 */
    private String stageName;

    /** 基础温度(℃)，低于此不累计积温 */
    private BigDecimal baseTemp;

    /** 本阶段目标积温(GDD) */
    private BigDecimal gddTarget;

    /** 最低温度(℃) */
    private BigDecimal tempMin;

    /** 最高温度(℃) */
    private BigDecimal tempMax;

    /** 最低空气湿度(%) */
    private Integer humidityMin;

    /** 最高空气湿度(%) */
    private Integer humidityMax;

    /** 最低光照(lux) */
    private Integer lightMin;

    /** 最高光照(lux) */
    private Integer lightMax;

    /** 最低土壤湿度(%) */
    private Integer soilMoistureMin;

    /** 最高土壤湿度(%) */
    private Integer soilMoistureMax;

    /** 灌溉量倍率 */
    private BigDecimal waterFactor;

    /** 补光强度倍率 */
    private BigDecimal lightFactor;

    /** 预计持续天数 */
    private Integer durationDays;

    /** 阶段排序 */
    private Integer sortOrder;
}
