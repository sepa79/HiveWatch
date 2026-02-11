package io.pockethive.hivewatch.service.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "hivewatch.auth")
public record HiveWatchAuthProperties(
        AuthMode mode,
        Jwt jwt
) {
    public record Jwt(
            String usernameClaim
    ) {
    }
}

