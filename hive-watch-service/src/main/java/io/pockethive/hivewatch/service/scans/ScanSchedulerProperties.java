package io.pockethive.hivewatch.service.scans;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "hivewatch.scans.scheduler")
public record ScanSchedulerProperties(
        boolean enabled,
        long fixedDelayMs,
        long initialDelayMs
) {
}

