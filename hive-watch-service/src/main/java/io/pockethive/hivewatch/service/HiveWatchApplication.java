package io.pockethive.hivewatch.service;

import io.pockethive.hivewatch.service.security.HiveWatchAuthProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(HiveWatchAuthProperties.class)
public class HiveWatchApplication {
    public static void main(String[] args) {
        SpringApplication.run(HiveWatchApplication.class, args);
    }
}
