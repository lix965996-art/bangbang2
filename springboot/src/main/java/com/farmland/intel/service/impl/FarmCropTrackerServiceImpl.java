package com.farmland.intel.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.farmland.intel.entity.FarmCropTracker;
import com.farmland.intel.mapper.FarmCropTrackerMapper;
import com.farmland.intel.service.IFarmCropTrackerService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class FarmCropTrackerServiceImpl extends ServiceImpl<FarmCropTrackerMapper, FarmCropTracker>
        implements IFarmCropTrackerService {

    @Override
    public FarmCropTracker getByFarmAndCrop(String farmName, String cropName) {
        return baseMapper.selectByFarmAndCrop(farmName, cropName);
    }

    @Override
    public List<FarmCropTracker> getNeedGddUpdate(LocalDate date) {
        return baseMapper.selectNeedGddUpdate(date);
    }

    @Override
    public List<FarmCropTracker> getAllTrackers() {
        return baseMapper.selectAllOrderByUpdateTime();
    }

    @Override
    public boolean updateGdd(Integer id, BigDecimal gdd, LocalDate date) {
        return baseMapper.updateGdd(id, gdd, date) > 0;
    }

    @Override
    public boolean updateStage(Integer id, String stage) {
        return baseMapper.updateStage(id, stage) > 0;
    }
}
