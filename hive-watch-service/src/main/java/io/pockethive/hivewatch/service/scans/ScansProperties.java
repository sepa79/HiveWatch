package io.pockethive.hivewatch.service.scans;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "hivewatch.scans")
public record ScansProperties(
        boolean manualEnabled
) {
}

