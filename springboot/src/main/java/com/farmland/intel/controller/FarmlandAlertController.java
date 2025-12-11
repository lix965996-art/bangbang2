package com.farmland.intel.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.farmland.intel.common.Result;
import com.farmland.intel.entity.FarmlandAlert;
import com.farmland.intel.mapper.FarmlandAlertMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 农田预警控制器
 */
@RestController
@RequestMapping("/alert")
@CrossOrigin
public class FarmlandAlertController {

    @Autowired
    private FarmlandAlertMapper alertMapper;

    /**
     * 获取今日任务清单（未处理的预警）
     */
    @GetMapping("/today/tasks")
    public Result getTodayTasks() {
        List<FarmlandAlert> alerts = alertMapper.selectTodayPendingAlerts();
        return Result.success(alerts);
    }

    /**
     * 获取所有未处理的预警
     */
    @GetMapping("/pending")
    public Result getPendingAlerts() {
        List<FarmlandAlert> alerts = alertMapper.selectList(
            new QueryWrapper<FarmlandAlert>()
                .eq("status", "pending")
                .orderByDesc("create_time")
        );
        return Result.success(alerts);
    }

    /**
     * 标记预警为已处理
     */
    @PostMapping("/{id}/process")
    public Result processAlert(@PathVariable Integer id, @RequestBody(required = false) FarmlandAlert alert) {
        FarmlandAlert existingAlert = alertMapper.selectById(id);
        if (existingAlert == null) {
            return Result.error("404", "预警不存在");
        }

        existingAlert.setStatus("processed");
        existingAlert.setProcessTime(LocalDateTime.now());
        if (alert != null && alert.getProcessor() != null) {
            existingAlert.setProcessor(alert.getProcessor());
        }

        alertMapper.updateById(existingAlert);
        return Result.success();
    }

    /**
     * 批量处理预警
     */
    @PostMapping("/batch/process")
    public Result batchProcessAlerts(@RequestBody List<Integer> ids) {
        for (Integer id : ids) {
            FarmlandAlert alert = alertMapper.selectById(id);
            if (alert != null && "pending".equals(alert.getStatus())) {
                alert.setStatus("processed");
                alert.setProcessTime(LocalDateTime.now());
                alertMapper.updateById(alert);
            }
        }
        return Result.success();
    }

    /**
     * 获取指定农田的预警列表
     */
    @GetMapping("/farmland/{farmlandId}")
    public Result getFarmlandAlerts(@PathVariable Integer farmlandId) {
        List<FarmlandAlert> alerts = alertMapper.selectList(
            new QueryWrapper<FarmlandAlert>()
                .eq("farmland_id", farmlandId)
                .orderByDesc("create_time")
        );
        return Result.success(alerts);
    }
}

