package com.farmland.intel.controller;

import com.farmland.intel.common.Result;
import com.farmland.intel.entity.SensorReading;
import com.farmland.intel.mapper.SensorReadingMapper;
import com.farmland.intel.service.IOneNetService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Aether 设备控制器。
 * 提供设备状态查询、LED 控制、历史数据查询和模拟数据写入能力。
 */
@RestController
@RequestMapping("/aether")
@CrossOrigin
public class AetherDeviceController {

    private static final Logger log = LoggerFactory.getLogger(AetherDeviceController.class);
    private static final String DEFAULT_DEVICE_NAME = "STM32-001";
    private static final String PUMP_DEVICE_NAME = "STM32-PUMP";

    private static Double currentTemperature = 25.5;
    private static Double currentHumidity = 60.0;
    private static Integer currentLed = 0;
    private static boolean deviceOnline = true;

    @Autowired
    private SensorReadingMapper sensorReadingMapper;

    @Autowired(required = false)
    private IOneNetService oneNetService;

    @GetMapping("/device/status")
    public Result getDeviceStatus() {
        Map<String, Object> data = new HashMap<>();

        if (oneNetService != null) {
            try {
                Map<String, Object> oneNetData = oneNetService.getDeviceData();
                if (Boolean.TRUE.equals(oneNetData.getOrDefault("success", false))) {
                    data.putAll(oneNetData);
                    data.put("device_name", DEFAULT_DEVICE_NAME);
                    data.put("source", "OneNET");

                    currentTemperature = toDouble(data.get("temperature"), currentTemperature);
                    currentHumidity = toDouble(data.get("humidity"), currentHumidity);
                    currentLed = toInteger(data.get("led"), currentLed);
                    deviceOnline = toBoolean(data.get("online"), true);

                    return Result.success(data);
                }
            } catch (Exception e) {
                log.warn("从 OneNET 获取设备状态失败: {}", e.getMessage());
            }
        }

        try {
            SensorReading latest = sensorReadingMapper.selectLatest();
            if (latest != null) {
                currentTemperature = latest.getTemperature();
                currentHumidity = latest.getHumidity();
                currentLed = latest.getLed();
                deviceOnline = true;

                data.put("success", true);
                data.put("online", true);
                data.put("temperature", currentTemperature);
                data.put("humidity", currentHumidity);
                data.put("led", currentLed);
                data.put("device_name", latest.getDeviceName());
                data.put("source", "Database");
                return Result.success(data);
            }
        } catch (Exception e) {
            log.warn("从数据库获取设备状态失败: {}", e.getMessage());
        }

        data.put("success", true);
        data.put("online", deviceOnline);
        data.put("temperature", currentTemperature);
        data.put("humidity", currentHumidity);
        data.put("led", currentLed);
        data.put("device_name", DEFAULT_DEVICE_NAME);
        data.put("source", "Cache");
        return Result.success(data);
    }

    @PostMapping("/device/control/led")
    public Result controlLed(@RequestBody Map<String, Object> params) {
        if (params == null || params.isEmpty()) {
            return Result.error("400", "请求参数不能为空");
        }

        Integer led = toInteger(params.get("led"), null);
        if (led == null || (led != 0 && led != 1)) {
            return Result.error("400", "LED 参数只能为 0 或 1");
        }

        boolean ledState = led == 1;
        boolean oneNetSuccess = false;
        if (oneNetService != null) {
            try {
                oneNetSuccess = oneNetService.controlLed(ledState);
                if (oneNetSuccess) {
                    log.info("OneNET LED 控制成功: {}", ledState ? "开启" : "关闭");
                }
            } catch (Exception e) {
                log.error("OneNET LED 控制失败", e);
            }
        }

        currentLed = led;
        persistSensorReading(currentTemperature, currentHumidity, currentLed, DEFAULT_DEVICE_NAME);

        Map<String, Object> data = new HashMap<>();
        data.put("success", true);
        data.put("led", led);
        data.put("message", ledState ? "LED 已开启" : "LED 已关闭");
        data.put("oneNetControlled", oneNetSuccess);
        return Result.success(data);
    }

    @GetMapping("/readings/detail")
    public Result getReadingsDetail(@RequestParam(defaultValue = "30") Integer days) {
        if (days == null || days < 1 || days > 365) {
            return Result.error("400", "天数参数必须在 1 到 365 之间");
        }

        try {
            LocalDateTime endTime = LocalDateTime.now();
            LocalDateTime startTime = endTime.minusDays(days);
            List<SensorReading> readings = sensorReadingMapper.selectByTimeRange(startTime, endTime);

            List<Map<String, Object>> data = new ArrayList<>();
            for (SensorReading reading : readings) {
                Map<String, Object> item = new HashMap<>();
                item.put("date", reading.getCreatedAt().toString());
                item.put("temp", reading.getTemperature());
                item.put("humi", reading.getHumidity());
                data.add(item);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", data);
            result.put("days", days);
            result.put("count", data.size());
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取历史数据失败", e);
            return Result.error("500", "数据库查询失败: " + e.getMessage());
        }
    }

    @PostMapping("/device/control/bump")
    public Result controlBump(@RequestBody Map<String, Object> params) {
        if (params == null || params.isEmpty()) {
            return Result.error("400", "请求参数不能为空");
        }

        Boolean bump = toBoolean(params.get("bump"), null);
        if (bump == null) {
            return Result.error("400", "bump 参数必须为布尔值或 0/1");
        }

        boolean oneNetSuccess = false;
        if (oneNetService != null) {
            try {
                oneNetSuccess = oneNetService.controlBump(bump);
                if (oneNetSuccess) {
                    log.info("OneNET 水泵控制成功: {}", bump ? "开启" : "关闭");
                }
            } catch (Exception e) {
                log.error("OneNET 水泵控制失败", e);
            }
        }

        Map<String, Object> data = new HashMap<>();
        data.put("success", true);
        data.put("bump", bump);
        data.put("message", bump ? "水泵已开启" : "水泵已关闭");
        data.put("oneNetControlled", oneNetSuccess);
        return Result.success(data);
    }

    @GetMapping("/device/new/status")
    public Result getNewDeviceStatus() {
        Map<String, Object> data = new HashMap<>();

        if (oneNetService != null) {
            try {
                Map<String, Object> oneNetData = oneNetService.getNewDeviceData();
                if (Boolean.TRUE.equals(oneNetData.getOrDefault("success", false))) {
                    data.putAll(oneNetData);
                    data.put("device_name", PUMP_DEVICE_NAME);
                    data.put("source", "OneNET");
                    return Result.success(data);
                }
            } catch (Exception e) {
                log.warn("从 OneNET 获取水泵设备状态失败: {}", e.getMessage());
            }
        }

        data.put("success", true);
        data.put("online", false);
        data.put("temperature", 25.0);
        data.put("humidity", 50.0);
        data.put("bump", false);
        data.put("device_name", PUMP_DEVICE_NAME);
        data.put("source", "Default");
        return Result.success(data);
    }

    @PostMapping("/device/simulate")
    public Result simulateData() {
        Random random = new Random();
        currentTemperature = 20 + random.nextDouble() * 10;
        currentHumidity = 50 + random.nextDouble() * 30;
        persistSensorReading(currentTemperature, currentHumidity, currentLed, DEFAULT_DEVICE_NAME);

        log.info("模拟数据生成成功: 温度={} 湿度={}", currentTemperature, currentHumidity);
        return Result.success("模拟数据已生成");
    }

    private void persistSensorReading(Double temperature, Double humidity, Integer led, String deviceName) {
        try {
            SensorReading reading = new SensorReading();
            reading.setTemperature(temperature);
            reading.setHumidity(humidity);
            reading.setLed(led);
            reading.setDeviceName(deviceName);
            reading.setCreatedAt(LocalDateTime.now());
            sensorReadingMapper.insert(reading);
        } catch (Exception e) {
            log.error("保存传感器数据失败", e);
        }
    }

    private Double toDouble(Object value, Double defaultValue) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException ignored) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private Integer toInteger(Object value, Integer defaultValue) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException ignored) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private Boolean toBoolean(Object value, Boolean defaultValue) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue() == 1;
        }
        if (value instanceof String) {
            String raw = ((String) value).trim();
            if ("1".equals(raw) || "true".equalsIgnoreCase(raw)) {
                return true;
            }
            if ("0".equals(raw) || "false".equalsIgnoreCase(raw)) {
                return false;
            }
        }
        return defaultValue;
    }
}
