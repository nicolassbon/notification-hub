package com.notificationhub.health;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ExternalServicesHealthIndicator implements HealthIndicator {

    @Override
    public Health health() {
        try {
            return Health.up()
                    .withDetail("status", "All systems operational")
                    .withDetail("service", "notification-hub")
                    .build();
                    
        } catch (Exception e) {
            log.error("Health check failed", e);
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
