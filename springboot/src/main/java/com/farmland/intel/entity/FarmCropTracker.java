package com.farmland.intel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 农场作物生长追踪
 */
@Data
@TableName("farm_crop_tracker")
public class FarmCropTracker implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /** 农场名称 */
    private String farmName;

    /** 作物名称 */
    private String cropName;

    /** 当前阶段 */
    private String currentStage;

    /** 播种日期 */
    private LocalDate plantingDate;

    /** 累计积温(GDD) */
    private BigDecimal accumulatedGdd;

    /** 上次积温更新日期 */
    private LocalDate lastGddUpdate;

    /** 预计采收日期 */
    private LocalDate expectedHarvest;

    /** 阶段更新时间 */
    private LocalDateTime stageUpdatedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
