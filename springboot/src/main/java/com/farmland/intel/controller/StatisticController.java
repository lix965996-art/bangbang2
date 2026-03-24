package com.farmland.intel.controller;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelWriter;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletOutputStream;
import java.net.URLEncoder;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.farmland.intel.entity.Statistic;
import com.farmland.intel.entity.User;
import com.farmland.intel.service.IStatisticService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.farmland.intel.common.Result;
import org.springframework.web.multipart.MultipartFile;
import com.farmland.intel.utils.TokenUtils;

import org.springframework.web.bind.annotation.RestController;

/**
 * 农田统计数据控制器
 * 提供农田数据的增删改查、导入导出、大屏展示等功能
 */
@RestController
@RequestMapping("/statistic")
public class StatisticController {

    private static final Logger log = LoggerFactory.getLogger(StatisticController.class);

    @Resource
    private IStatisticService statisticService;

    @Resource
    private com.farmland.intel.service.IHealthIndexService healthIndexService;

    /**
     * 新增或更新农田统计数据
     */
    @PostMapping
    public Result save(@RequestBody Statistic statistic) {
        // 参数验证
        if (statistic == null) {
            return Result.error("400", "统计数据不能为空");
        }
        
        log.info("📥 接收到保存请求 - 农田: {}, 区县: {}", statistic.getFarm(), statistic.getDistrict());
        
        // 数据验证：温度范围检查（仅在明确提交温度数据时验证）
        if (statistic.getTemperature() != null) {
            double temp = statistic.getTemperature().doubleValue();
            // 检查温度是否在合理范围内
            if (temp < -20 || temp > 50) {
                // 如果温度异常，自动修正为null，不阻止保存
                log.warn("温度数据异常，已自动清除: {}°C (农田: {})", temp, statistic.getFarm());
                statistic.setTemperature(null);
            }
        }
        
        // 数据验证：湿度范围检查（自动修正异常值）
        if (statistic.getAirhumidity() != null && (statistic.getAirhumidity() < 0 || statistic.getAirhumidity() > 100)) {
            log.warn("空气湿度数据异常，已自动清除: {}% (农田: {})", statistic.getAirhumidity(), statistic.getFarm());
            statistic.setAirhumidity(null);
        }
        
        if (statistic.getSoilhumidity() != null && (statistic.getSoilhumidity() < 0 || statistic.getSoilhumidity() > 100)) {
            log.warn("土壤湿度数据异常，已自动清除: {}% (农田: {})", statistic.getSoilhumidity(), statistic.getFarm());
            statistic.setSoilhumidity(null);
        }
        
        try {
            // 设置创建时间（新增时）
            if (statistic.getId() == null) {
                // 自动设置农田负责人
                try {
                    User currentUser = TokenUtils.getCurrentUser();
                    if (currentUser != null) {
                        statistic.setKeeper(resolveKeeperName(currentUser));
                    }
                } catch (Exception e) {
                    // ignore
                }
            }
            
            statisticService.saveOrUpdate(statistic);
            log.info("农田统计数据保存成功，ID: {}", statistic.getId());
            
            // 计算健康指数并生成预警（暂时禁用，避免错误）
            try {
                if (healthIndexService != null) {
                    Integer healthIndex = healthIndexService.calculateHealthIndex(statistic);
                    healthIndexService.checkAndGenerateAlerts(statistic);
                    log.debug("健康指数计算完成: {}", healthIndex);
                }
            } catch (Exception e) {
                log.warn("健康指数计算失败（不影响保存）", e);
            }
            
            return Result.success();
        } catch (Exception e) {
            log.error("保存农田统计数据失败", e);
            return Result.error("500", "保存失败，请重试");
        }
    }

    private void applyKeeperDataPermission(QueryWrapper<Statistic> queryWrapper) {
        User currentUser = TokenUtils.getCurrentUser();
        if (currentUser == null || "ROLE_ADMIN".equals(currentUser.getRole())) {
            return;
        }

        String username = StrUtil.trimToEmpty(currentUser.getUsername());
        String nickname = StrUtil.trimToEmpty(currentUser.getNickname());
        if (StrUtil.isBlank(username) && StrUtil.isBlank(nickname)) {
            queryWrapper.eq("keeper", "__NO_MATCH__");
            return;
        }

        if (StrUtil.isNotBlank(username) && StrUtil.isNotBlank(nickname) && !StrUtil.equals(username, nickname)) {
            queryWrapper.and(wrapper -> wrapper.eq("keeper", username).or().eq("keeper", nickname));
            return;
        }

        queryWrapper.eq("keeper", StrUtil.blankToDefault(username, nickname));
    }

    private String resolveKeeperName(User currentUser) {
        if (currentUser == null) {
            return null;
        }
        return StrUtil.blankToDefault(StrUtil.trim(currentUser.getNickname()), StrUtil.trim(currentUser.getUsername()));
    }

    /**
     * 删除农田统计数据
     */
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id) {
        if (id == null || id <= 0) {
            return Result.error("400", "ID参数无效");
        }
        
        try {
            statisticService.removeById(id);
            log.info("删除农田统计数据成功，ID: {}", id);
            return Result.success();
        } catch (Exception e) {
            log.error("删除农田统计数据失败，ID: {}", id, e);
            return Result.error("500", "删除失败，请重试");
        }
    }

    /**
     * 批量删除农田统计数据
     */
    @PostMapping("/del/batch")
    public Result deleteBatch(@RequestBody List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return Result.error("400", "删除ID列表不能为空");
        }
        
        try {
            statisticService.removeByIds(ids);
            log.info("批量删除农田统计数据成功，数量: {}", ids.size());
            return Result.success();
        } catch (Exception e) {
            log.error("批量删除农田统计数据失败", e);
            return Result.error("500", "批量删除失败，请重试");
        }
    }

    @GetMapping
    public Result findAll() {
        QueryWrapper<Statistic> queryWrapper = new QueryWrapper<>();
        // 非管理员只能查看自己负责的农田
        User currentUser = TokenUtils.getCurrentUser();
        applyKeeperDataPermission(queryWrapper);
        return Result.success(statisticService.list(queryWrapper));
    }

    @GetMapping("/{id}")
    public Result findOne(@PathVariable Integer id) {
        return Result.success(statisticService.getById(id));
    }

    @GetMapping("/page")
    public Result findPage(@RequestParam(defaultValue = "") String farm,
                           @RequestParam Integer pageNum,
                           @RequestParam Integer pageSize) {
        QueryWrapper<Statistic> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByDesc("id");
        if (!"".equals(farm)) {
            queryWrapper.like("farm", farm);
        }
        
        // 数据权限控制：非管理员只能看自己负责的农田
        User currentUser = TokenUtils.getCurrentUser();
        applyKeeperDataPermission(queryWrapper);
        
        return Result.success(statisticService.page(new Page<>(pageNum, pageSize), queryWrapper));
    }

    /**
    * 导出接口
    */
    @GetMapping("/export")
    public void export(HttpServletResponse response) throws Exception {
        // 从数据库查询出所有的数据
        List<Statistic> list = statisticService.list();
        // 在内存操作，写出到浏览器
        ExcelWriter writer = ExcelUtil.getWriter(true);

        // 一次性写出list内的对象到excel，使用默认样式，强制输出标题
        writer.write(list, true);

        // 设置浏览器响应的格式
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=utf-8");
        String fileName = URLEncoder.encode("Statistic信息表", "UTF-8");
        response.setHeader("Content-Disposition", "attachment;filename=" + fileName + ".xlsx");

        ServletOutputStream out = response.getOutputStream();
        writer.flush(out, true);
        out.close();
        writer.close();

        }

    /**
     * excel 导入
     * @param file
     * @throws Exception
     */
    @PostMapping("/import")
    public Result imp(MultipartFile file) throws Exception {
        InputStream inputStream = file.getInputStream();
        ExcelReader reader = ExcelUtil.getReader(inputStream);
        // 通过 javabean的方式读取Excel内的对象，但是要求表头必须是英文，跟javabean的属性要对应起来
        List<Statistic> list = reader.readAll(Statistic.class);

        statisticService.saveBatch(list);
        return Result.success();
    }

    /**
     * 获取当前登录用户（暂未使用）
     */
    @SuppressWarnings("unused")
    private User getUser() {
        return TokenUtils.getCurrentUser();
    }

    /**
     * 大屏数据接口 - 获取农田信息用于大屏展示
     * 非管理员只能查看自己负责的农田
     */
    @GetMapping("/dashboard")
    public Result getDashboardData() {
        try {
            QueryWrapper<Statistic> queryWrapper = new QueryWrapper<>();
            // 非管理员只能查看自己负责的农田
            User currentUser = TokenUtils.getCurrentUser();
            applyKeeperDataPermission(queryWrapper);
            List<Statistic> list = statisticService.list(queryWrapper);
            return Result.success(list);
        } catch (Exception e) {
            log.error("获取Dashboard数据失败", e);
            // 返回空列表而不是500错误
            return Result.success(new java.util.ArrayList<>());
        }
    }

    /**
     * 大屏数据接口 - 获取统计汇总数据
     * 非管理员只能查看自己负责的农田统计
     */
    @GetMapping("/dashboard/summary")
    public Result getDashboardSummary() {
        try {
            QueryWrapper<Statistic> queryWrapper = new QueryWrapper<>();
            // 非管理员只能查看自己负责的农田
            User currentUser = TokenUtils.getCurrentUser();
            applyKeeperDataPermission(queryWrapper);
            List<Statistic> list = statisticService.list(queryWrapper);
            
            // 计算总面积（area是String类型，需要转换）
            int totalArea = list.stream()
                    .filter(s -> s.getArea() != null && !s.getArea().isEmpty())
                    .mapToInt(s -> {
                        try {
                            return Integer.parseInt(s.getArea());
                        } catch (NumberFormatException e) {
                            return 0;
                        }
                    })
                    .sum();
            
            // 计算总数量（存栏）
            int totalStock = list.stream()
                    .filter(s -> s.getNumber() != null)
                    .mapToInt(Statistic::getNumber)
                    .sum();
            
            // 统计数据
            Map<String, Object> summary = new HashMap<>();
            summary.put("totalArea", totalArea);
            summary.put("totalStock", totalStock);
            summary.put("farmCount", list.size());
            summary.put("normalCount", list.stream().filter(s -> "正常".equals(s.getState())).count());
            
            return Result.success(summary);
        } catch (Exception e) {
            log.error("获取Dashboard汇总数据失败", e);
            // 返回默认值
            Map<String, Object> summary = new HashMap<>();
            summary.put("totalArea", 0);
            summary.put("totalStock", 0);
            summary.put("farmCount", 0);
            summary.put("normalCount", 0);
            return Result.success(summary);
        }
    }

    /**
     * 数据修正接口 - 修正异常的温度数据
     * 将超出正常范围的温度值修正为合理值
     */
    @PostMapping("/fix-temperature")
    public Result fixTemperatureData() {
        try {
            List<Statistic> list = statisticService.list();
            int fixedCount = 0;
            
            for (Statistic statistic : list) {
                if (statistic.getTemperature() != null) {
                    double temp = statistic.getTemperature().doubleValue();
                    
                    // 检测异常温度：正常农业环境温度范围 -20°C ~ 50°C
                    if (temp < -20 || temp > 50) {
                        // 如果温度异常，修正为合理的默认值
                        // 可以根据实际情况设置，这里设置为 25°C（适宜温度）
                        statistic.setTemperature(new java.math.BigDecimal("25.0"));
                        statisticService.updateById(statistic);
                        fixedCount++;
                        log.info("修正异常温度数据 - ID: {}, 农田: {}, 原温度: {}°C -> 修正为: 25.0°C", 
                                statistic.getId(), statistic.getFarm(), temp);
                    }
                }
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("totalCount", list.size());
            result.put("fixedCount", fixedCount);
            result.put("message", "温度数据修正完成，共修正 " + fixedCount + " 条异常数据");
            
            log.info("温度数据修正完成，总数: {}, 修正数: {}", list.size(), fixedCount);
            return Result.success(result);
        } catch (Exception e) {
            log.error("修正温度数据失败", e);
            return Result.error("500", "修正失败: " + e.getMessage());
        }
    }

    /**
     * 数据修正接口 - 自动填充老数据的区县字段
     * 根据地址信息自动识别并填充district字段
     */
    @PostMapping("/fix-district")
    public Result fixDistrictData() {
        try {
            List<Statistic> list = statisticService.list();
            int fixedCount = 0;
            
            for (Statistic statistic : list) {
                // 只处理district字段为空或为"其他区域"的记录
                if ((statistic.getDistrict() == null || 
                     statistic.getDistrict().trim().isEmpty() || 
                     "其他区域".equals(statistic.getDistrict())) && 
                    statistic.getAddress() != null && 
                    !statistic.getAddress().trim().isEmpty()) {
                    
                    String address = statistic.getAddress();
                    String district = extractDistrictFromAddress(address);
                    
                    if (district != null && !"其他区域".equals(district)) {
                        statistic.setDistrict(district);
                        statisticService.updateById(statistic);
                        fixedCount++;
                        log.info("自动填充区县数据 - ID: {}, 农田: {}, 地址: {} -> 区县: {}", 
                                statistic.getId(), statistic.getFarm(), address, district);
                    }
                }
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("totalCount", list.size());
            result.put("fixedCount", fixedCount);
            result.put("message", "区县数据修正完成，共填充 " + fixedCount + " 条记录");
            
            log.info("区县数据修正完成，总数: {}, 修正数: {}", list.size(), fixedCount);
            return Result.success(result);
        } catch (Exception e) {
            log.error("修正区县数据失败", e);
            return Result.error("500", "修正失败: " + e.getMessage());
        }
    }
    
    /**
     * 从地址中提取区县信息（参考Django版本的逻辑）
     */
    private String extractDistrictFromAddress(String address) {
        if (address == null || address.trim().isEmpty()) {
            return "未知区县";  // 参考Django版本，使用"未知区县"而不是"其他区域"
        }
        
        address = address.trim();
        
        // 1. 对于学校、景点等POI，优先使用特殊映射（参考Django POI搜索逻辑）
        java.util.Map<String, String> poiMapping = new java.util.HashMap<>();
        poiMapping.put("张家界学院", "张家界市永定区");
        poiMapping.put("吉首大学张家界", "张家界市永定区");
        poiMapping.put("湖南中医药大学", "长沙市岳麓区");
        poiMapping.put("中南大学", "长沙市岳麓区");
        poiMapping.put("湖南大学", "长沙市岳麓区");
        poiMapping.put("湖南师范大学", "长沙市岳麓区");
        
        // 检查是否包含学校、景点等关键词
        if (address.contains("学院") || address.contains("大学") || address.contains("学校") || 
            address.contains("景点") || address.contains("公园") || address.contains("广场")) {
            for (java.util.Map.Entry<String, String> entry : poiMapping.entrySet()) {
                if (address.contains(entry.getKey())) {
                    return entry.getValue();
                }
            }
        }
        
        // 2. 优先匹配完整的"市+区县"格式
        java.util.regex.Pattern fullPattern = java.util.regex.Pattern.compile("([\\u4e00-\\u9fa5]{2,4}市[\\u4e00-\\u9fa5]{2,3}[区县])");
        java.util.regex.Matcher fullMatcher = fullPattern.matcher(address);
        if (fullMatcher.find()) {
            return fullMatcher.group(1);
        }
        
        // 3. 组合匹配：市+区县
        java.util.regex.Pattern cityDistrictPattern = java.util.regex.Pattern.compile("([\\u4e00-\\u9fa5]{2,4}市)([\\u4e00-\\u9fa5]{2,3}[区县])");
        java.util.regex.Matcher cityDistrictMatcher = cityDistrictPattern.matcher(address);
        if (cityDistrictMatcher.find()) {
            return cityDistrictMatcher.group(1) + cityDistrictMatcher.group(2);
        }
        
        // 4. 张家界市下辖区县映射（参考Django的get_districts接口）
        java.util.Map<String, String> zjjDistrictMapping = new java.util.HashMap<>();
        zjjDistrictMapping.put("永定区", "张家界市永定区");
        zjjDistrictMapping.put("武陵源区", "张家界市武陵源区");
        zjjDistrictMapping.put("慈利县", "张家界市慈利县");
        zjjDistrictMapping.put("桑植县", "张家界市桑植县");
        
        // 5. 长沙市区县映射
        java.util.Map<String, String> csDistrictMapping = new java.util.HashMap<>();
        csDistrictMapping.put("岳麓区", "长沙市岳麓区");
        csDistrictMapping.put("芙蓉区", "长沙市芙蓉区");
        csDistrictMapping.put("天心区", "长沙市天心区");
        csDistrictMapping.put("开福区", "长沙市开福区");
        csDistrictMapping.put("雨花区", "长沙市雨花区");
        csDistrictMapping.put("望城区", "长沙市望城区");
        
        // 先检查张家界区县
        for (java.util.Map.Entry<String, String> entry : zjjDistrictMapping.entrySet()) {
            if (address.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        
        // 再检查长沙区县
        for (java.util.Map.Entry<String, String> entry : csDistrictMapping.entrySet()) {
            if (address.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        
        // 6. 包含"张家界"但没匹配到具体区县的，默认永定区（参考Django逻辑）
        if (address.contains("张家界")) {
            return "张家界市永定区";
        }
        
        // 7. 包含"长沙"但没匹配到具体区县的，默认岳麓区
        if (address.contains("长沙")) {
            return "长沙市岳麓区";
        }
        
        return "未知区县";  // 参考Django版本
    }
}
