package com.farmland.intel.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.farmland.intel.common.Constants;
import com.farmland.intel.common.Result;
import com.farmland.intel.entity.CropGrowthProfile;
import com.farmland.intel.entity.FarmCropTracker;
import com.farmland.intel.entity.Statistic;
import com.farmland.intel.entity.User;
import com.farmland.intel.service.CropGrowthService;
import com.farmland.intel.service.ICropGrowthProfileService;
import com.farmland.intel.service.IFarmCropTrackerService;
import com.farmland.intel.service.IStatisticService;
import com.farmland.intel.utils.TokenUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 作物生长追踪 API
 */
@RestController
@RequestMapping("/api/crop-growth")
public class CropGrowthController {

    private static final Logger log = LoggerFactory.getLogger(CropGrowthController.class);

    @Resource
    private IFarmCropTrackerService trackerService;

    @Resource
    private ICropGrowthProfileService profileService;

    @Resource
    private CropGrowthService cropGrowthService;

    @Resource
    private IStatisticService statisticService;

    // ==================== 追踪记录 ====================

    /** 获取所有追踪记录（含阶段详情） */
    @GetMapping("/trackers")
    public Result getAllTrackers() {
        List<FarmCropTracker> trackers = trackerService.getAllTrackers();
        List<Map<String, Object>> result = trackers.stream().map(t -> {
            Map<String, Object> map = trackerToMap(t);
            enrichWithStageInfo(map, t);
            enrichWithFarmData(map, t);
            return map;
        }).collect(Collectors.toList());
        return Result.success(result);
    }

    /** 按农场查询追踪记录 */
    @GetMapping("/tracker/{farmName}")
    public Result getTrackerByFarm(@PathVariable String farmName) {
        List<FarmCropTracker> trackers = trackerService.list(
                new QueryWrapper<FarmCropTracker>().eq("farm_name", farmName));
        if (trackers.isEmpty()) {
            return Result.error("404", "未找到该农场的追踪记录");
        }
        List<Map<String, Object>> result = trackers.stream().map(t -> {
            Map<String, Object> map = trackerToMap(t);
            enrichWithStageInfo(map, t);
            enrichWithFarmData(map, t);
            return map;
        }).collect(Collectors.toList());
        return Result.success(result);
    }

    /** 创建追踪记录（播种） */
    @PostMapping("/tracker")
    public Result createTracker(@RequestBody FarmCropTracker tracker) {
        User currentUser = TokenUtils.getCurrentUser();
        if (currentUser == null || !"ROLE_ADMIN".equals(currentUser.getRole())) {
            return Result.error(Constants.CODE_401, "无权限，仅管理员可操作");
        }
        if (tracker.getFarmName() == null || tracker.getCropName() == null) {
            return Result.error("400", "农场名称和作物名称不能为空");
        }

        // 检查是否已存在
        FarmCropTracker existing = trackerService.getByFarmAndCrop(
                tracker.getFarmName(), tracker.getCropName());
        if (existing != null) {
            return Result.error("409", "该农场已存在此作物的追踪记录");
        }

        // 检查作物配置是否存在
        List<CropGrowthProfile> stages = profileService.getByCropName(tracker.getCropName());
        if (stages.isEmpty()) {
            return Result.error("400", "未找到作物 " + tracker.getCropName() + " 的阶段配置");
        }

        tracker.setCurrentStage("seeding");
        tracker.setAccumulatedGdd(BigDecimal.ZERO);
        tracker.setPlantingDate(tracker.getPlantingDate() != null ? tracker.getPlantingDate() : LocalDate.now());
        tracker.setStageUpdatedAt(LocalDateTime.now());

        // 计算预计采收日
        LocalDate harvest = cropGrowthService.predictHarvestDate(tracker, stages);
        tracker.setExpectedHarvest(harvest);

        trackerService.save(tracker);
        return Result.success(trackerToMap(tracker));
    }

    /** 更新追踪记录 */
    @PutMapping("/tracker/{id}")
    public Result updateTracker(@PathVariable Integer id, @RequestBody FarmCropTracker tracker) {
        User currentUser = TokenUtils.getCurrentUser();
        if (currentUser == null || !"ROLE_ADMIN".equals(currentUser.getRole())) {
            return Result.error(Constants.CODE_401, "无权限，仅管理员可操作");
        }
        FarmCropTracker existing = trackerService.getById(id);
        if (existing == null) {
            return Result.error("404", "追踪记录不存在");
        }

        if (tracker.getPlantingDate() != null) {
            existing.setPlantingDate(tracker.getPlantingDate());
        }
        if (tracker.getCurrentStage() != null) {
            existing.setCurrentStage(tracker.getCurrentStage());
            existing.setStageUpdatedAt(LocalDateTime.now());
        }

        trackerService.updateById(existing);
        return Result.success(trackerToMap(existing));
    }

    /** 删除追踪记录 */
    @DeleteMapping("/tracker/{id}")
    public Result deleteTracker(@PathVariable Integer id) {
        User currentUser = TokenUtils.getCurrentUser();
        if (currentUser == null || !"ROLE_ADMIN".equals(currentUser.getRole())) {
            return Result.error(Constants.CODE_401, "无权限，仅管理员可操作");
        }
        if (!trackerService.removeById(id)) {
            return Result.error("404", "追踪记录不存在");
        }
        return Result.success();
    }

    // ==================== 阶段配置 ====================

    /** 获取所有作物的阶段配置 */
    @GetMapping("/profiles")
    public Result getAllProfiles() {
        return Result.success(profileService.list(
                new QueryWrapper<CropGrowthProfile>().orderByAsc("crop_name", "sort_order")));
    }

    /** 按作物查询阶段配置 */
    @GetMapping("/profile/{cropName}")
    public Result getProfilesByCrop(@PathVariable String cropName) {
        return Result.success(profileService.getByCropName(cropName));
    }

    /** 获取已配置的作物名列表 */
    @GetMapping("/crop-names")
    public Result getCropNames() {
        return Result.success(profileService.getAllCropNames());
    }

    /** 新增或更新阶段配置 */
    @PostMapping("/profile")
    public Result saveProfile(@RequestBody CropGrowthProfile profile) {
        User currentUser = TokenUtils.getCurrentUser();
        if (currentUser == null || !"ROLE_ADMIN".equals(currentUser.getRole())) {
            return Result.error(Constants.CODE_401, "无权限，仅管理员可操作");
        }
        if (profile.getCropName() == null || profile.getStage() == null) {
            return Result.error("400", "作物名称和阶段标识不能为空");
        }
        profileService.saveOrUpdate(profile);
        return Result.success(profile);
    }

    /** 删除阶段配置 */
    @DeleteMapping("/profile/{id}")
    public Result deleteProfile(@PathVariable Integer id) {
        User currentUser = TokenUtils.getCurrentUser();
        if (currentUser == null || !"ROLE_ADMIN".equals(currentUser.getRole())) {
            return Result.error(Constants.CODE_401, "无权限，仅管理员可操作");
        }
        if (!profileService.removeById(id)) {
            return Result.error("404", "阶段配置不存在");
        }
        return Result.success();
    }

    // ==================== 运维接口 ====================

    /** 为已有农田批量初始化追踪记录 */
    @PostMapping("/init")
    public Result initTrackers() {
        User currentUser = TokenUtils.getCurrentUser();
        if (currentUser == null || !"ROLE_ADMIN".equals(currentUser.getRole())) {
            return Result.error(Constants.CODE_401, "无权限，仅管理员可操作");
        }
        int created = cropGrowthService.initTrackersForExistingFarms();
        return Result.success("初始化完成，创建 " + created + " 条追踪记录");
    }

    /** 手动触发积温更新（调试用） */
    @PostMapping("/refresh-gdd")
    public Result refreshGdd() {
        User currentUser = TokenUtils.getCurrentUser();
        if (currentUser == null || !"ROLE_ADMIN".equals(currentUser.getRole())) {
            return Result.error(Constants.CODE_401, "无权限，仅管理员可操作");
        }
        cropGrowthService.dailyGddUpdate();
        return Result.success("积温更新已触发");
    }

    /** 获取总览统计 */
    @GetMapping("/overview")
    public Result getOverview() {
        List<FarmCropTracker> trackers = trackerService.list();
        Map<String, Object> overview = new HashMap<>();
        overview.put("totalTrackers", trackers.size());

        // 按阶段统计
        Map<String, Long> stageDistribution = trackers.stream()
                .collect(Collectors.groupingBy(FarmCropTracker::getCurrentStage, Collectors.counting()));
        overview.put("stageDistribution", stageDistribution);

        // 统计即将采收的
        long nearHarvest = trackers.stream()
                .filter(t -> "harvest".equals(t.getCurrentStage()) || "fruiting".equals(t.getCurrentStage()))
                .count();
        overview.put("nearHarvest", nearHarvest);

        // 统计作物种类
        long cropTypes = trackers.stream()
                .map(FarmCropTracker::getCropName)
                .distinct()
                .count();
        overview.put("cropTypes", cropTypes);

        return Result.success(overview);
    }

    // ==================== 辅助方法 ====================

    private Map<String, Object> trackerToMap(FarmCropTracker t) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", t.getId());
        map.put("farmName", t.getFarmName());
        map.put("cropName", t.getCropName());
        map.put("currentStage", t.getCurrentStage());
        map.put("plantingDate", t.getPlantingDate());
        map.put("accumulatedGdd", t.getAccumulatedGdd());
        map.put("lastGddUpdate", t.getLastGddUpdate());
        map.put("expectedHarvest", t.getExpectedHarvest());
        map.put("stageUpdatedAt", t.getStageUpdatedAt());
        return map;
    }

    private void enrichWithStageInfo(Map<String, Object> map, FarmCropTracker t) {
        List<CropGrowthProfile> stages = profileService.getByCropName(t.getCropName());
        if (stages.isEmpty()) return;

        // 当前阶段配置
        CropGrowthProfile currentStage = cropGrowthService.determineStage(stages, t.getAccumulatedGdd());
        if (currentStage != null) {
            map.put("stageName", currentStage.getStageName());
            map.put("stageIndex", currentStage.getSortOrder());
            map.put("totalStages", stages.size());
            map.put("progress", calculateProgress(stages, t.getAccumulatedGdd()));
            map.put("waterFactor", currentStage.getWaterFactor());
            map.put("lightFactor", currentStage.getLightFactor());
            map.put("idealTempMin", currentStage.getTempMin());
            map.put("idealTempMax", currentStage.getTempMax());
            map.put("idealSoilMoistureMin", currentStage.getSoilMoistureMin());
            map.put("idealSoilMoistureMax", currentStage.getSoilMoistureMax());
        }
    }

    private void enrichWithFarmData(Map<String, Object> map, FarmCropTracker t) {
        Statistic farm = statisticService.getOne(
                new QueryWrapper<Statistic>().eq("farm", t.getFarmName()).last("LIMIT 1"));
        if (farm != null) {
            map.put("currentTemp", farm.getTemperature());
            map.put("currentSoilHumidity", farm.getSoilhumidity());
            map.put("currentAirHumidity", farm.getAirhumidity());
            map.put("currentLight", farm.getLight());
        }
    }

    /** 计算生长进度百分比 */
    private BigDecimal calculateProgress(List<CropGrowthProfile> stages, BigDecimal accumulatedGdd) {
        BigDecimal totalGdd = stages.stream()
                .map(CropGrowthProfile::getGddTarget)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalGdd.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return accumulatedGdd.divide(totalGdd, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .min(BigDecimal.valueOf(100))
                .setScale(1, RoundingMode.HALF_UP);
    }
}
