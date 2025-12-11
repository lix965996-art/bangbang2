package com.farmland.intel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.farmland.intel.entity.InventoryOutbound;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

/**
 * 物资出库记录Mapper
 */
@Mapper
public interface InventoryOutboundMapper extends BaseMapper<InventoryOutbound> {
    
    /**
     * 计算指定时间范围内的日均消耗量
     */
    @Select("SELECT COALESCE(SUM(quantity), 0) / GREATEST(DATEDIFF(#{endTime}, #{startTime}), 1) " +
            "FROM inventory_outbound " +
            "WHERE inventory_id = #{inventoryId} AND outbound_time >= #{startTime} AND outbound_time <= #{endTime}")
    Double calculateDailyConsumption(@org.apache.ibatis.annotations.Param("inventoryId") Integer inventoryId, 
                                     @org.apache.ibatis.annotations.Param("startTime") LocalDateTime startTime, 
                                     @org.apache.ibatis.annotations.Param("endTime") LocalDateTime endTime);
}

