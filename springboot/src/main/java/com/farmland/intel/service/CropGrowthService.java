package com.farmland.intel.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.farmland.intel.entity.CropGrowthProfile;
import com.farmland.intel.entity.FarmCropTracker;
import com.farmland.intel.entity.Statistic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 作物生长服务 — 积温计算、阶段推进、采收预测
 */
@Service
public class CropGrowthService {

    private static final Logger log = LoggerFactory.getLogger(CropGrowthService.class);

    @Resource
    private IFarmCropTrackerService trackerService;

    @Resource
    private ICropGrowthProfileService profileService;

    @Resource
    private IStatisticService statisticService;

    /**
     * 计算单日 GDD（Growing Degree Days）
     * GDD = max(0, (Tmax + Tmin) / 2 - Tbase)
     */
    public BigDecimal calculateDailyGdd(BigDecimal dayMaxTemp, BigDecimal dayMinTemp, BigDecimal baseTemp) {
        if (dayMaxTemp == null || dayMinTemp == null || baseTemp == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal avg = dayMaxTemp.add(dayMinTemp).divide(BigDecimal.valueOf(2), 1, RoundingMode.HALF_UP);
        BigDecimal gdd = avg.subtract(baseTemp);
        return gdd.compareTo(BigDecimal.ZERO) > 0 ? gdd.setScale(1, RoundingMode.HALF_UP) : BigDecimal.ZERO;
    }

    /**
     * 根据累计积温确定当前阶段
     * 逐阶段累加 gddTarget，看累计积温落在哪个阶段
     */
    public CropGrowthProfile determineStage(List<CropGrowthProfile> stages, BigDecimal accumulatedGdd) {
        if (stages == null || stages.isEmpty()) {
            return null;
        }

        BigDecimal cumulativeGdd = BigDecimal.ZERO;
        CropGrowthProfile currentStage = stages.get(0);

        for (CropGrowthProfile stage : stages) {
            cumulativeGdd = cumulativeGdd.add(stage.getGddTarget());
            if (accumulatedGdd.compareTo(cumulativeGdd) < 0) {
                return stage;
            }
            currentStage = stage;
        }

        // 积温超过所有阶段总和，返回最后一个阶段
        return currentStage;
    }

    /**
     * 预测采收日期
     * 根据当前积温与总需求积温的比例，估算剩余天数
     */
    public LocalDate predictHarvestDate(FarmCropTracker tracker, List<CropGrowthProfile> stages) {
        if (stages == null || stages.isEmpty() || tracker.getPlantingDate() == null) {
            return null;
        }

        BigDecimal totalGdd = stages.stream()
                .map(CropGrowthProfile::getGddTarget)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int totalDays = stages.stream()
                .filter(s -> s.getDurationDays() != null)
                .mapToInt(CropGrowthProfile::getDurationDays)
                .sum();

        if (totalGdd.compareTo(BigDecimal.ZERO) == 0 || totalDays == 0) {
            return null;
        }

        // 已过天数
        long elapsedDays = ChronoUnit.DAYS.between(tracker.getPlantingDate(), LocalDate.now());

        // 积温完成比例
        BigDecimal progress = tracker.getAccumulatedGdd()
                .divide(totalGdd, 4, RoundingMode.HALF_UP)
                .min(BigDecimal.ONE);

        // 预计总天数 = 已过天数 / 积温完成比例
        if (progress.compareTo(BigDecimal.ZERO) == 0) {
            return tracker.getPlantingDate().plusDays(totalDays);
        }

        BigDecimal estimatedTotalDays = BigDecimal.valueOf(elapsedDays)
                .divide(progress, 0, RoundingMode.HALF_UP);

        long remainingDays = estimatedTotalDays.longValue() - elapsedDays;
        return LocalDate.now().plusDays(Math.max(remainingDays, 0));
    }

    /**
     * 获取指定农场当前阶段的配置（用于自动化联动）
     */
    public CropGrowthProfile getCurrentStageConfig(String farmName) {
        Statistic farm = statisticService.getOne(
                new QueryWrapper<Statistic>().eq("farm", farmName).last("LIMIT 1"));
        if (farm == null || farm.getCrop() == null) {
            return null;
        }

        FarmCropTracker tracker = trackerService.getByFarmAndCrop(farmName, farm.getCrop());
        if (tracker == null) {
            return null;
        }

        List<CropGrowthProfile> stages = profileService.getByCropName(tracker.getCropName());
        return determineStage(stages, tracker.getAccumulatedGdd());
    }

    /**
     * 获取指定农场作物的灌溉量倍率
     */
    public BigDecimal getWaterFactor(String farmName) {
        CropGrowthProfile config = getCurrentStageConfig(farmName);
        return config != null && config.getWaterFactor() != null ? config.getWaterFactor() : BigDecimal.ONE;
    }

    /**
     * 获取指定农场作物的补光倍率
     */
    public BigDecimal getLightFactor(String farmName) {
        CropGrowthProfile config = getCurrentStageConfig(farmName);
        return config != null && config.getLightFactor() != null ? config.getLightFactor() : BigDecimal.ONE;
    }

    /**
     * 每日凌晨2点执行：更新所有追踪记录的积温和阶段
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void dailyGddUpdate() {
        log.info("[CropGrowth] 开始每日积温更新");
        LocalDate today = LocalDate.now();

        List<FarmCropTracker> trackers = trackerService.list();
        if (trackers.isEmpty()) {
            log.info("[CropGrowth] 无追踪记录，跳过");
            return;
        }

        int updated = 0;
        int stageChanged = 0;

        for (FarmCropTracker tracker : trackers) {
            try {
                // 跳过今天已更新的
                if (today.equals(tracker.getLastGddUpdate())) {
                    continue;
                }

                // 获取农场当前温度（从statistic表取）
                Statistic farm = statisticService.getOne(
                        new QueryWrapper<Statistic>().eq("farm", tracker.getFarmName()).last("LIMIT 1"));
                if (farm == null || farm.getTemperature() == null) {
                    log.warn("[CropGrowth] 农场 {} 无温度数据，跳过", tracker.getFarmName());
                    continue;
                }

                // 获取作物阶段配置，取基础温度
                List<CropGrowthProfile> stages = profileService.getByCropName(tracker.getCropName());
                if (stages == null || stages.isEmpty()) {
                    log.warn("[CropGrowth] 作物 {} 无阶段配置，跳过", tracker.getCropName());
                    continue;
                }

                CropGrowthProfile currentStageConfig = determineStage(stages, tracker.getAccumulatedGdd());
                BigDecimal baseTemp = currentStageConfig != null && currentStageConfig.getBaseTemp() != null
                        ? currentStageConfig.getBaseTemp()
                        : stages.get(0).getBaseTemp();

                // 计算 GDD：用当前温度作为日均温的近似值
                BigDecimal currentTemp = farm.getTemperature();
                BigDecimal dailyGdd = calculateDailyGdd(currentTemp, currentTemp, baseTemp);

                // 累加积温
                BigDecimal newGdd = tracker.getAccumulatedGdd()
                        .add(dailyGdd)
                        .setScale(1, RoundingMode.HALF_UP);

                trackerService.updateGdd(tracker.getId(), newGdd, today);

                // 检查阶段是否变化
                String oldStage = tracker.getCurrentStage();
                CropGrowthProfile newStageConfig = determineStage(stages, newGdd);
                if (newStageConfig != null && !newStageConfig.getStage().equals(oldStage)) {
                    trackerService.updateStage(tracker.getId(), newStageConfig.getStage());
                    log.info("[CropGrowth] 农场 {} 作物 {} 阶段变更: {} -> {}",
                            tracker.getFarmName(), tracker.getCropName(), oldStage, newStageConfig.getStage());
                    stageChanged++;
                }

                // 更新预计采收日
                LocalDate harvestDate = predictHarvestDate(tracker, stages);
                if (harvestDate != null) {
                    tracker.setExpectedHarvest(harvestDate);
                    trackerService.updateById(tracker);
                }

                updated++;
            } catch (Exception e) {
                log.error("[CropGrowth] 更新农场 {} 失败", tracker.getFarmName(), e);
            }
        }

        log.info("[CropGrowth] 积温更新完成: 更新{}, 阶段变更{}", updated, stageChanged);
    }

    /**
     * 初始化农场作物追踪：为已有农田自动创建追踪记录
     */
    public int initTrackersForExistingFarms() {
        List<Statistic> farms = statisticService.list();
        int created = 0;

        for (Statistic farm : farms) {
            if (farm.getCrop() == null || farm.getCrop().trim().isEmpty()) {
                continue;
            }

            // 处理混种（如"番茄,玉米"）
            String[] crops = farm.getCrop().split(",");
            for (String crop : crops) {
                String cropName = crop.trim();
                if (cropName.isEmpty()) continue;

                // 检查是否已有追踪记录
                FarmCropTracker existing = trackerService.getByFarmAndCrop(farm.getFarm(), cropName);
                if (existing != null) continue;

                // 检查是否有阶段配置
                List<CropGrowthProfile> stages = profileService.getByCropName(cropName);
                if (stages == null || stages.isEmpty()) {
                    log.warn("[CropGrowth] 作物 {} 无阶段配置，跳过初始化", cropName);
                    continue;
                }

                FarmCropTracker tracker = new FarmCropTracker();
                tracker.setFarmName(farm.getFarm());
                tracker.setCropName(cropName);
                tracker.setCurrentStage("seeding");
                tracker.setPlantingDate(LocalDate.now());
                tracker.setAccumulatedGdd(BigDecimal.ZERO);
                tracker.setStageUpdatedAt(LocalDateTime.now());

                trackerService.save(tracker);
                created++;
                log.info("[CropGrowth] 创建追踪: {} - {}", farm.getFarm(), cropName);
            }
        }

        return created;
    }
}
