package com.farmland.intel.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.farmland.intel.entity.CropGrowthProfile;

import java.util.List;

public interface ICropGrowthProfileService extends IService<CropGrowthProfile> {

    /** 按作物名查询所有阶段配置（按排序） */
    List<CropGrowthProfile> getByCropName(String cropName);

    /** 获取所有已配置的作物名 */
    List<String> getAllCropNames();
}
