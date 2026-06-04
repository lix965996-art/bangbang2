package com.farmland.intel.controller;

import org.springframework.transaction.annotation.Transactional;
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelWriter;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.ServletOutputStream;
import java.net.URLEncoder;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.farmland.intel.entity.User;
import com.farmland.intel.service.IInventoryService;
import org.springframework.web.bind.annotation.*;
import jakarta.annotation.Resource;
import java.io.InputStream;
import java.util.List;
import java.util.ArrayList;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.farmland.intel.common.Constants;
import com.farmland.intel.common.Result;
import org.springframework.web.multipart.MultipartFile;
import com.farmland.intel.utils.TokenUtils;

import com.farmland.intel.entity.Inventory;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.Map;
import java.util.HashMap;

import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 *  前端控制器
 * </p>
 */
@RestController
@RequestMapping("/inventory")
public class InventoryController {

    @Resource
    private IInventoryService inventoryService;

    // 新增或者更新
    @PostMapping
    public Result save(@RequestBody Inventory inventory) {
        User currentUser = TokenUtils.getCurrentUser();
        if (currentUser == null) {
            return Result.error("401", "未登录");
        }
        if (inventory.getId() == null) {
            // 新增时自动填充仓库管理员为当前登录用户
            inventory.setKeeper(currentUser.getUsername());
        } else {
            // 更新：校验权限（管理员或原仓库管理员）
            if (!"ROLE_ADMIN".equals(currentUser.getRole())) {
                Inventory existing = inventoryService.getById(inventory.getId());
                if (existing == null) {
                    return Result.error("404", "记录不存在");
                }
                if (!currentUser.getUsername().equals(existing.getKeeper())) {
                    return Result.error(Constants.CODE_401, "无权限修改该记录");
                }
            }
        }
        inventoryService.saveOrUpdate(inventory);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id) {
        User currentUser = TokenUtils.getCurrentUser();
        if (currentUser == null) {
            return Result.error("401", "未登录");
        }
        if (!"ROLE_ADMIN".equals(currentUser.getRole())) {
            Inventory entity = inventoryService.getById(id);
            if (entity == null) {
                return Result.error("404", "记录不存在");
            }
            if (!currentUser.getUsername().equals(entity.getKeeper())) {
                return Result.error(Constants.CODE_401, "无权限删除该记录");
            }
        }
        inventoryService.removeById(id);
        return Result.success();
    }

    @PostMapping("/del/batch")
    public Result deleteBatch(@RequestBody List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return Result.error("400", "删除ID列表不能为空");
        }
        User currentUser = TokenUtils.getCurrentUser();
        if (currentUser == null) {
            return Result.error("401", "未登录");
        }
        if (!"ROLE_ADMIN".equals(currentUser.getRole())) {
            List<Inventory> entities = (List<Inventory>) inventoryService.listByIds(ids);
            for (Inventory entity : entities) {
                if (!currentUser.getUsername().equals(entity.getKeeper())) {
                    return Result.error(Constants.CODE_401, "无权限删除记录: " + entity.getId());
                }
            }
        }
        inventoryService.removeByIds(ids);
        return Result.success();
    }

    @GetMapping
    public Result findAll() {
        QueryWrapper<Inventory> queryWrapper = new QueryWrapper<>();
        // 非管理员只能查看自己负责的库存
        User currentUser = TokenUtils.getCurrentUser();
        if (currentUser != null && !"ROLE_ADMIN".equals(currentUser.getRole())) {
            queryWrapper.eq("keeper", currentUser.getUsername());
        }
        return Result.success(inventoryService.list(queryWrapper));
    }

    @GetMapping("/{id}")
    public Result findOne(@PathVariable Integer id) {
        Inventory inventory = inventoryService.getById(id);
        if (inventory == null) {
            return Result.error("404", "记录不存在");
        }
        User currentUser = TokenUtils.getCurrentUser();
        if (currentUser != null && !"ROLE_ADMIN".equals(currentUser.getRole())
                && !currentUser.getUsername().equals(inventory.getKeeper())) {
            return Result.error("403", "无权限查看该记录");
        }
        return Result.success(inventory);
    }

    @GetMapping("/page")
    public Result findPage(@RequestParam(defaultValue = "") String produce,
                           @RequestParam Integer pageNum,
                           @RequestParam Integer pageSize) {
        QueryWrapper<Inventory> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByDesc("id");
        if (!"".equals(produce)) {
            queryWrapper.like("produce", produce);
        }
        // 非管理员只能查看自己负责的库存
        User currentUser = TokenUtils.getCurrentUser();
        if (currentUser != null && !"ROLE_ADMIN".equals(currentUser.getRole())) {
            queryWrapper.eq("keeper", currentUser.getUsername());
        }
        return Result.success(inventoryService.page(new Page<>(pageNum, pageSize), queryWrapper));
    }

    /**
    * 导出接口
    */
    @GetMapping("/export")
    public void export(HttpServletResponse response) throws Exception {
        // 设置浏览器响应的格式
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=utf-8");
        String fileName = URLEncoder.encode("Inventory信息表", "UTF-8");
        response.setHeader("Content-Disposition", "attachment;filename=" + fileName + ".xlsx");

        try (ServletOutputStream out = response.getOutputStream();
             ExcelWriter writer = ExcelUtil.getWriter(true)) {

            // 分批查询，避免一次性加载所有数据导致OOM
            int pageSize = 1000;
            int pageNum = 1;
            Page<Inventory> page;
            boolean isFirstPage = true;

            // 构建查询条件（非管理员只能导出自己负责的库存）
            QueryWrapper<Inventory> queryWrapper = new QueryWrapper<>();
            User currentUser = TokenUtils.getCurrentUser();
            if (currentUser != null && !"ROLE_ADMIN".equals(currentUser.getRole())) {
                queryWrapper.eq("keeper", currentUser.getUsername());
            }

            do {
                page = inventoryService.page(new Page<>(pageNum, pageSize), queryWrapper);
                List<Inventory> list = page.getRecords();

                if (list == null || list.isEmpty()) {
                    break;
                }

                writer.write(list, isFirstPage);
                isFirstPage = false;
                pageNum++;

            } while (page.hasNext());

            writer.flush(out, true);
        }
    }

    /**
     * excel 导入
     * @param file
     * @throws Exception
     */
    @Transactional
    @PostMapping("/import")
    public Result imp(MultipartFile file) throws Exception {
        User currentUser = TokenUtils.getCurrentUser();
        if (currentUser == null || !"ROLE_ADMIN".equals(currentUser.getRole())) {
            return Result.error(Constants.CODE_401, "无权限，仅管理员可操作");
        }
        if (file == null || file.isEmpty()) {
            return Result.error("400", "请上传Excel文件");
        }
        if (file.getSize() > 5 * 1024 * 1024) {
            return Result.error("400", "Excel文件不能超过5MB");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || (!originalFilename.toLowerCase().endsWith(".xlsx") && !originalFilename.toLowerCase().endsWith(".xls"))) {
            return Result.error("400", "仅支持 .xlsx 或 .xls 格式的Excel文件");
        }
        try (InputStream inputStream = file.getInputStream();
             ExcelReader reader = ExcelUtil.getReader(inputStream)) {
            List<Inventory> list = reader.readAll(Inventory.class);
            if (list == null || list.isEmpty()) {
                return Result.error("400", "Excel中没有可导入的数据");
            }
            if (list.size() > 1000) {
                return Result.error("400", "单次最多导入1000条库存记录");
            }
            List<Inventory> validList = new ArrayList<>();
            for (int i = 0; i < list.size(); i++) {
                Inventory item = list.get(i);
                if (item == null) {
                    continue;
                }
                if (item.getProduce() == null || item.getProduce().trim().isEmpty()) {
                    return Result.error("400", "第" + (i + 1) + "行产品名称不能为空");
                }
                if (item.getWarehouse() == null || item.getWarehouse().trim().isEmpty()) {
                    return Result.error("400", "第" + (i + 1) + "行仓库不能为空");
                }
                if (item.getNumber() == null || item.getNumber() < 0) {
                    return Result.error("400", "第" + (i + 1) + "行数量必须为非负整数");
                }
                if (item.getSafeStock() != null && item.getSafeStock() < 0) {
                    return Result.error("400", "第" + (i + 1) + "行安全库存不能为负数");
                }
                if (item.getMaxStock() != null && item.getMaxStock() < 0) {
                    return Result.error("400", "第" + (i + 1) + "行最大库存不能为负数");
                }
                if (item.getKeeper() == null || item.getKeeper().trim().isEmpty()) {
                    item.setKeeper(currentUser.getUsername());
                }
                validList.add(item);
            }
            if (validList.isEmpty()) {
                return Result.error("400", "Excel中没有有效库存记录");
            }
            inventoryService.saveBatch(validList);
        }
        return Result.success();
    }

    private User getUser() {
        return TokenUtils.getCurrentUser();
    }

    @Autowired
    private com.farmland.intel.mapper.InventoryOutboundMapper inventoryOutboundMapper;

    /**
     * 获取需要补货的物资列表
     */
    @GetMapping("/replenishment-alerts")
    public Result getReplenishmentAlerts(@RequestParam(defaultValue = "7") Integer days) {
        List<Inventory> inventories = inventoryService.list();
        List<Map<String, Object>> alerts = new java.util.ArrayList<>();
        
        java.time.LocalDateTime endTime = java.time.LocalDateTime.now();
        java.time.LocalDateTime startTime = endTime.minusDays(days);
        
        // 批量查询所有库存的日均消耗，避免N+1查询问题
        List<Integer> inventoryIds = new java.util.ArrayList<>();
        for (Inventory inventory : inventories) {
            if (inventory.getId() != null) {
                inventoryIds.add(inventory.getId());
            }
        }
        
        // 批量查询所有库存的日均消耗
        Map<Integer, Double> dailyConsumptionMap = new java.util.HashMap<>();
        if (!inventoryIds.isEmpty()) {
            List<Map<String, Object>> consumptionList = inventoryOutboundMapper.calculateDailyConsumptionBatch(
                inventoryIds, startTime, endTime);
            for (Map<String, Object> row : consumptionList) {
                Integer inventoryId = ((Number) row.get("inventory_id")).intValue();
                Double consumption = ((Number) row.get("daily_consumption")).doubleValue();
                dailyConsumptionMap.put(inventoryId, consumption);
            }
        }
        
        for (Inventory inventory : inventories) {
            // 检查安全库存
            if (inventory.getSafeStock() != null && inventory.getNumber() != null 
                && inventory.getNumber() < inventory.getSafeStock()) {
                Map<String, Object> alert = new java.util.HashMap<>();
                alert.put("inventoryId", inventory.getId());
                alert.put("produce", inventory.getProduce());
                alert.put("warehouse", inventory.getWarehouse());
                alert.put("currentStock", inventory.getNumber());
                alert.put("safeStock", inventory.getSafeStock());
                alert.put("alertType", "safe_stock");
                alert.put("message", String.format("%s当前库存%d，低于安全库存%d", 
                    inventory.getProduce(), inventory.getNumber(), inventory.getSafeStock()));
                alerts.add(alert);
                continue;
            }
            
            // 计算日均消耗和预计可用天数
            if (inventory.getId() != null) {
                Double dailyConsumption = dailyConsumptionMap.get(inventory.getId());
                
                if (dailyConsumption != null && dailyConsumption > 0 
                    && inventory.getNumber() != null) {
                    int daysAvailable = (int) (inventory.getNumber() / dailyConsumption);
                    
                    // 如果预计可用天数小于指定天数，生成预警
                    if (daysAvailable < days) {
                        Map<String, Object> alert = new java.util.HashMap<>();
                        alert.put("inventoryId", inventory.getId());
                        alert.put("produce", inventory.getProduce());
                        alert.put("warehouse", inventory.getWarehouse());
                        alert.put("currentStock", inventory.getNumber());
                        alert.put("dailyConsumption", String.format("%.2f", dailyConsumption));
                        alert.put("daysAvailable", daysAvailable);
                        alert.put("alertType", "low_stock");
                        alert.put("message", String.format("%s预计可用%d天，建议及时补货", 
                            inventory.getProduce(), daysAvailable));
                        alerts.add(alert);
                    }
                }
            }
        }
        
        return Result.success(alerts);
    }

}
