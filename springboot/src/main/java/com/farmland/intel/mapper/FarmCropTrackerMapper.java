package com.farmland.intel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.farmland.intel.entity.FarmCropTracker;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Mapper
public interface FarmCropTrackerMapper extends BaseMapper<FarmCropTracker> {

    @Select("SELECT * FROM farm_crop_tracker WHERE farm_name = #{farmName} AND crop_name = #{cropName}")
    FarmCropTracker selectByFarmAndCrop(@Param("farmName") String farmName, @Param("cropName") String cropName);

    @Select("SELECT * FROM farm_crop_tracker WHERE last_gdd_update < #{date} OR last_gdd_update IS NULL")
    List<FarmCropTracker> selectNeedGddUpdate(LocalDate date);

    @Select("SELECT * FROM farm_crop_tracker ORDER BY updated_at DESC")
    List<FarmCropTracker> selectAllOrderByUpdateTime();

    @Update("UPDATE farm_crop_tracker SET accumulated_gdd = #{gdd}, last_gdd_update = #{date} WHERE id = #{id}")
    int updateGdd(@Param("id") Integer id, @Param("gdd") BigDecimal gdd, @Param("date") LocalDate date);

    @Update("UPDATE farm_crop_tracker SET current_stage = #{stage}, stage_updated_at = NOW() WHERE id = #{id}")
    int updateStage(@Param("id") Integer id, @Param("stage") String stage);
}
