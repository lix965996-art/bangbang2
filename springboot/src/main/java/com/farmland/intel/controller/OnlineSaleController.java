package com.farmland.intel.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.farmland.intel.common.Constants;
import com.farmland.intel.common.Result;
import com.farmland.intel.entity.Inventory;
import com.farmland.intel.entity.OnlineSale;
import com.farmland.intel.entity.User;
import com.farmland.intel.service.IInventoryService;
import com.farmland.intel.service.IOnlineSaleService;
import com.farmland.intel.utils.TokenUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.util.List;

/**
 * 农作物在线销售Controller
 */
@RestController
@RequestMapping("/onlineSale")
public class OnlineSaleController {

    @Resource
    private IOnlineSaleService onlineSaleService;

    @Resource
    private IInventoryService inventoryService;

    // 新增或更新
    @Transactional
    @PostMapping
    public Result save(@RequestBody OnlineSale onlineSale) {
        // 校验库存数量
        if (onlineSale.getInventoryId() != null) {
            Inventory inventory = inventoryService.getById(onlineSale.getInventoryId());
            if (inventory == null) {
                return Result.error("400", "库存商品不存在");
            }
            int qty = onlineSale.getQuantity() != null ? onlineSale.getQuantity() : 0;
            if (qty < 1) {
                return Result.error("400", "上架数量至少为 1");
            }
            // 自动填充商品信息
            if (onlineSale.getId() == null) {
                onlineSale.setProduce(inventory.getProduce());
                onlineSale.setWarehouse(inventory.getWarehouse());
            }
        }

        // 自动填充销售员 (仅新增时) + 更新权限校验
        User currentUser = TokenUtils.getCurrentUser();
        if (onlineSale.getId() == null) {
            if (currentUser != null) {
                onlineSale.setSeller(currentUser.getUsername());
            }
        } else {
            OnlineSale existing = onlineSaleService.getById(onlineSale.getId());
            if (existing == null) {
                return Result.error("404", "记录不存在");
            }
            // 更新：校验权限（管理员或原销售员）
            if (currentUser != null && !"ROLE_ADMIN".equals(currentUser.getRole())) {
                if (!currentUser.getUsername().equals(existing.getSeller())) {
                    return Result.error(Constants.CODE_401, "无权限修改该记录");
                }
            }
        }

        // 计算总价
        if (onlineSale.getPrice() != null && onlineSale.getQuantity() != null) {
            onlineSale.setTotalPrice(onlineSale.getPrice().multiply(new BigDecimal(onlineSale.getQuantity())));
        }

        boolean isNew = onlineSale.getId() == null;
        if (isNew && onlineSale.getInventoryId() != null) {
            int qty = onlineSale.getQuantity() != null ? onlineSale.getQuantity() : 0;
            if (!deductInventory(onlineSale.getInventoryId(), qty)) {
                return Result.error("400", "库存不足，无法创建销售记录");
            }
        } else if (!isNew) {
            OnlineSale existing = onlineSaleService.getById(onlineSale.getId());
            Result inventoryAdjustResult = adjustInventoryForUpdate(existing, onlineSale);
            if (inventoryAdjustResult != null) {
                return inventoryAdjustResult;
            }
        }
        onlineSaleService.saveOrUpdate(onlineSale);

        return Result.success();
    }

    @Transactional
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id) {
        User currentUser = TokenUtils.getCurrentUser();
        if (currentUser == null) {
            return Result.error("401", "未登录");
        }
        OnlineSale entity = onlineSaleService.getById(id);
        if (entity == null) {
            return Result.error("404", "记录不存在");
        }
        if (!"ROLE_ADMIN".equals(currentUser.getRole())) {
            if (!currentUser.getUsername().equals(entity.getSeller())) {
                return Result.error(Constants.CODE_401, "无权限删除该记录");
            }
        }
        restoreInventory(entity);
        onlineSaleService.removeById(id);
        return Result.success();
    }

    @Transactional
    @PostMapping("/del/batch")
    public Result deleteBatch(@RequestBody List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return Result.error("400", "删除ID列表不能为空");
        }
        User currentUser = TokenUtils.getCurrentUser();
        if (currentUser == null) {
            return Result.error("401", "未登录");
        }
        List<OnlineSale> entities = onlineSaleService.listByIds(ids);
        if (entities.size() != ids.size()) {
            return Result.error("404", "部分销售记录不存在");
        }
        if (!"ROLE_ADMIN".equals(currentUser.getRole())) {
            for (OnlineSale entity : entities) {
                if (!currentUser.getUsername().equals(entity.getSeller())) {
                    return Result.error(Constants.CODE_401, "无权限删除记录: " + entity.getId());
                }
            }
        }
        for (OnlineSale entity : entities) {
            restoreInventory(entity);
        }
        onlineSaleService.removeByIds(ids);
        return Result.success();
    }

    // 查询全部
    @GetMapping
    public Result findAll() {
        QueryWrapper<OnlineSale> queryWrapper = new QueryWrapper<>();
        // 非管理员只能查看自己的在线销售记录
        User currentUser = TokenUtils.getCurrentUser();
        if (currentUser != null && !"ROLE_ADMIN".equals(currentUser.getRole())) {
            queryWrapper.eq("seller", currentUser.getUsername());
        }
        return Result.success(onlineSaleService.list(queryWrapper));
    }

    // 根据ID查询
    @GetMapping("/{id}")
    public Result findOne(@PathVariable Integer id) {
        OnlineSale sale = onlineSaleService.getById(id);
        if (sale == null) return Result.error("404", "记录不存在");
        // 数据权限控制：非管理员只能查看自己的
        User currentUser = TokenUtils.getCurrentUser();
        if (currentUser != null && !"ROLE_ADMIN".equals(currentUser.getRole())) {
            if (!currentUser.getUsername().equals(sale.getSeller())) {
                return Result.error("403", "无权限查看");
            }
        }
        return Result.success(sale);
    }

    // 分页查询
    @GetMapping("/page")
    public Result findPage(@RequestParam(defaultValue = "") String produce,
                           @RequestParam Integer pageNum,
                           @RequestParam Integer pageSize) {
        QueryWrapper<OnlineSale> queryWrapper = new QueryWrapper<>();
        queryWrapper.like(!"".equals(produce), "produce", produce);
        queryWrapper.orderByDesc("id");

        // 数据权限控制：非管理员只能看自己的
        User currentUser = TokenUtils.getCurrentUser();
        if (currentUser != null && !"ROLE_ADMIN".equals(currentUser.getRole())) {
            queryWrapper.eq("seller", currentUser.getUsername());
        }

        Page<OnlineSale> page = onlineSaleService.page(new Page<>(pageNum, pageSize), queryWrapper);
        return Result.success(page);
    }

    // 修改状态
    private static final java.util.Set<String> ALLOWED_STATUS = java.util.Set.of("上架中", "已下架", "已售罄");

    @PutMapping("/status/{id}")
    public Result updateStatus(@PathVariable Integer id, @RequestParam String status) {
        if (!ALLOWED_STATUS.contains(status)) {
            return Result.error("400", "无效的状态值，允许: " + ALLOWED_STATUS);
        }
        OnlineSale sale = onlineSaleService.getById(id);
        if (sale == null) {
            return Result.error("404", "销售记录不存在");
        }
        User currentUser = TokenUtils.getCurrentUser();
        if (currentUser == null) {
            return Result.error("401", "未登录");
        }
        if (!"ROLE_ADMIN".equals(currentUser.getRole())
                && !currentUser.getUsername().equals(sale.getSeller())) {
            return Result.error(Constants.CODE_401, "无权限修改该记录状态");
        }
        sale.setStatus(status);
        onlineSaleService.updateById(sale);
        return Result.success();
    }

    private void restoreInventory(OnlineSale sale) {
        if (sale == null || sale.getInventoryId() == null || sale.getQuantity() == null || sale.getQuantity() <= 0) {
            return;
        }
        inventoryService.update(new UpdateWrapper<Inventory>()
                .setSql("number = number + " + sale.getQuantity())
                .eq("id", sale.getInventoryId()));
    }

    private boolean deductInventory(Integer inventoryId, int quantity) {
        if (inventoryId == null || quantity <= 0) {
            return true;
        }
        return inventoryService.update(new UpdateWrapper<Inventory>()
                .setSql("number = number - " + quantity)
                .eq("id", inventoryId)
                .ge("number", quantity));
    }

    private Result adjustInventoryForUpdate(OnlineSale oldSale, OnlineSale newSale) {
        if (oldSale == null) {
            return Result.error("404", "记录不存在");
        }

        Integer oldInventoryId = oldSale.getInventoryId();
        Integer newInventoryId = newSale.getInventoryId();
        int oldQty = oldSale.getQuantity() != null ? oldSale.getQuantity() : 0;
        int newQty = newSale.getQuantity() != null ? newSale.getQuantity() : 0;

        if (newQty < 1) {
            return Result.error("400", "上架数量至少为 1");
        }

        if (java.util.Objects.equals(oldInventoryId, newInventoryId)) {
            int delta = newQty - oldQty;
            if (delta > 0 && !deductInventory(newInventoryId, delta)) {
                return Result.error("400", "库存不足，无法增加销售数量");
            }
            if (delta < 0) {
                restoreInventoryFor(newInventoryId, -delta);
            }
            return null;
        }

        if (newInventoryId != null && !deductInventory(newInventoryId, newQty)) {
            return Result.error("400", "库存不足，无法切换销售商品");
        }
        restoreInventory(oldSale);

        Inventory inventory = newInventoryId != null ? inventoryService.getById(newInventoryId) : null;
        if (inventory != null) {
            newSale.setProduce(inventory.getProduce());
            newSale.setWarehouse(inventory.getWarehouse());
        }
        return null;
    }

    private void restoreInventoryFor(Integer inventoryId, int quantity) {
        if (inventoryId == null || quantity <= 0) {
            return;
        }
        inventoryService.update(new UpdateWrapper<Inventory>()
                .setSql("number = number + " + quantity)
                .eq("id", inventoryId));
    }

    // 导出Excel
    @GetMapping("/export")
    public void export(HttpServletResponse response) throws Exception {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=utf-8");
        String fileName = URLEncoder.encode("农作物在线销售", "UTF-8");
        response.setHeader("Content-Disposition", "attachment;filename=" + fileName + ".xlsx");

        try (ServletOutputStream out = response.getOutputStream();
             ExcelWriter writer = ExcelUtil.getWriter(true)) {
            writer.addHeaderAlias("id", "ID");
            writer.addHeaderAlias("produce", "商品名称");
            writer.addHeaderAlias("warehouse", "所属仓库");
            writer.addHeaderAlias("quantity", "出售数量");
            writer.addHeaderAlias("price", "单价(元)");
            writer.addHeaderAlias("totalPrice", "总价(元)");
            writer.addHeaderAlias("status", "状态");
            writer.addHeaderAlias("seller", "销售员");
            writer.addHeaderAlias("createTime", "创建时间");
            writer.addHeaderAlias("remark", "备注");

            // 数据权限控制：非管理员只能导出自己的
            QueryWrapper<OnlineSale> exportQw = new QueryWrapper<>();
            User currentUser = TokenUtils.getCurrentUser();
            if (currentUser != null && !"ROLE_ADMIN".equals(currentUser.getRole())) {
                exportQw.eq("seller", currentUser.getUsername());
            }

            int pageSize = 1000;
            int pageNum = 1;
            Page<OnlineSale> page;
            boolean isFirstPage = true;

            do {
                page = onlineSaleService.page(new Page<>(pageNum, pageSize), exportQw);
                List<OnlineSale> list = page.getRecords();

                if (CollUtil.isEmpty(list)) {
                    break;
                }

                writer.write(list, isFirstPage);
                isFirstPage = false;
                pageNum++;

            } while (page.hasNext());

            writer.flush(out, true);
        }
    }
}
