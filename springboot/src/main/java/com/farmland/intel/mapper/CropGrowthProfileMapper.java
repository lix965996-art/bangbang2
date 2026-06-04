package com.farmland.intel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.farmland.intel.entity.CropGrowthProfile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CropGrowthProfileMapper extends BaseMapper<CropGrowthProfile> {

    @Select("SELECT * FROM crop_growth_profile WHERE crop_name = #{cropName} ORDER BY sort_order ASC")
    List<CropGrowthProfile> selectByCropName(String cropName);

    @Select("SELECT DISTINCT crop_name FROM crop_growth_profile ORDER BY crop_name")
    List<String> selectAllCropNames();
}
