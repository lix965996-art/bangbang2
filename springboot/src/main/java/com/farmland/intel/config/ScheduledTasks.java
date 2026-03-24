package com.farmland.intel.config;

import com.farmland.intel.service.IOneNetService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
public class ScheduledTasks {

    private static final Logger log = LoggerFactory.getLogger(ScheduledTasks.class);

    @Autowired(required = false)
    private IOneNetService oneNetService;

    @Value("${onenet.enabled:true}")
    private boolean oneNetEnabled;

    @Scheduled(fixedRate = 30000)
    public void syncOneNetData() {
        if (!oneNetEnabled || oneNetService == null) {
            log.debug("OneNET service disabled, skip scheduled sync");
            return;
        }

        try {
            boolean success1 = oneNetService.syncDataToDatabase();
            if (!success1) {
                log.debug("Old OneNET device sync failed");
            }
        } catch (Exception e) {
            log.warn("Old OneNET device sync error", e);
        }

        try {
            boolean success2 = oneNetService.syncNewDeviceDataToDatabase();
            if (!success2) {
                log.debug("New OneNET device sync failed");
            }
        } catch (Exception e) {
            log.warn("New OneNET device sync error", e);
        }
    }
}
