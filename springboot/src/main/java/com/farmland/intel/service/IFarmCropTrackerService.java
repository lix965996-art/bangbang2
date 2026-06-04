package com.farmland.intel.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.farmland.intel.entity.FarmCropTracker;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface IFarmCropTrackerService extends IService<FarmCropTracker> {

    /** 查询指定农场的作物追踪 */
    FarmCropTracker getByFarmAndCrop(String farmName, String cropName);

    /** 获取需要更新积温的追踪记录 */
    List<FarmCropTracker> getNeedGddUpdate(LocalDate date);

    /** 获取所有追踪记录 */
    List<FarmCropTracker> getAllTrackers();

    /** 更新积温 */
    boolean updateGdd(Integer id, BigDecimal gdd, LocalDate date);

    /** 更新阶段 */
    boolean updateStage(Integer id, String stage);
}
