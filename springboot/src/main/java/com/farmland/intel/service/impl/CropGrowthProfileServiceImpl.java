package com.farmland.intel.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.farmland.intel.entity.CropGrowthProfile;
import com.farmland.intel.mapper.CropGrowthProfileMapper;
import com.farmland.intel.service.ICropGrowthProfileService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CropGrowthProfileServiceImpl extends ServiceImpl<CropGrowthProfileMapper, CropGrowthProfile>
        implements ICropGrowthProfileService {

    @Override
    public List<CropGrowthProfile> getByCropName(String cropName) {
        return baseMapper.selectByCropName(cropName);
    }

    @Override
    public List<String> getAllCropNames() {
        return baseMapper.selectAllCropNames();
    }
}
