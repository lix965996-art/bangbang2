package com.farmland.intel.config;

import com.farmland.intel.service.IOneNetService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * 定时任务配置
 * 用于定期同步OneNET数据到本地数据库
 */
@Configuration
@EnableScheduling
public class ScheduledTasks {
    
    private static final Logger log = LoggerFactory.getLogger(ScheduledTasks.class);
    
    @Autowired(required = false)
    private IOneNetService oneNetService;
    
    /**
     * 每30秒从OneNET同步一次传感器数据到数据库
     * 同时同步两个设备：
     * 1. 旧设备 CzL61ga8FI (STM32-001) - 温湿度 + LED
     * 2. 新设备 KK57iNOm8d (STM32-PUMP) - 温湿度 + LED + 水泵
     */
    @Scheduled(fixedRate = 30000) // 30秒
    public void syncOneNetData() {
        if (oneNetService != null) {
            // 同步旧设备数据 (CzL61ga8FI)
            try {
                boolean success1 = oneNetService.syncDataToDatabase();
                if (!success1) {
                    log.debug("⚠️ 旧设备(CzL61ga8FI)数据同步失败");
                } else {
                    log.debug("✅ 旧设备(CzL61ga8FI)数据同步成功");
                }
            } catch (Exception e) {
                log.warn("⚠️ 旧设备数据同步异常", e);
            }
            
            // 同步新设备数据 (KK57iNOm8d)
            try {
                boolean success2 = oneNetService.syncNewDeviceDataToDatabase();
                if (!success2) {
                    log.debug("⚠️ 新设备(KK57iNOm8d)数据同步失败");
                } else {
                    log.debug("✅ 新设备(KK57iNOm8d)数据同步成功");
                }
            } catch (Exception e) {
                log.warn("⚠️ 新设备数据同步异常", e);
            }
        } else {
            log.debug("OneNET服务未启用，跳过数据同步");
        }
    }
}

