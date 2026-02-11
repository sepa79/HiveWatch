package io.pockethive.hivewatch.service;

import io.pockethive.hivewatch.service.security.HiveWatchAuthProperties;
import io.pockethive.hivewatch.service.scans.ScanSchedulerProperties;
import io.pockethive.hivewatch.service.scans.ScansProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties({HiveWatchAuthProperties.class, ScanSchedulerProperties.class, ScansProperties.class})
@EnableScheduling
public class HiveWatchApplication {
    public static void main(String[] args) {
        SpringApplication.run(HiveWatchApplication.class, args);
    }
}
