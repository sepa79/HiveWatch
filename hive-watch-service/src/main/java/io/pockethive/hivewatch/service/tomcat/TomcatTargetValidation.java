package io.pockethive.hivewatch.service.tomcat;

import io.pockethive.hivewatch.service.targets.TargetConnectionValidation;
import java.net.URI;
import java.net.URISyntaxException;

public final class TomcatTargetValidation {
    private TomcatTargetValidation() {
    }

    public static URI parseBaseUrl(String rawBaseUrl) {
        if (rawBaseUrl == null || rawBaseUrl.trim().isBlank()) {
            throw new IllegalArgumentException("baseUrl is required");
        }
        URI base;
        try {
            base = URI.create(rawBaseUrl.trim());
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("baseUrl is invalid");
        }
        if (!base.isAbsolute() || base.getHost() == null || base.getHost().isBlank()) {
            throw new IllegalArgumentException("baseUrl must be absolute and include host");
        }
        if (!"http".equalsIgnoreCase(base.getScheme()) && !"https".equalsIgnoreCase(base.getScheme())) {
            throw new IllegalArgumentException("baseUrl scheme must be http/https");
        }
        if (base.getUserInfo() != null) {
            throw new IllegalArgumentException("baseUrl must not include userinfo");
        }
        if (base.getPort() != -1) {
            throw new IllegalArgumentException("baseUrl must not include port; use explicit port");
        }
        String path = base.getPath();
        if (path != null && !path.isBlank() && !"/".equals(path)) {
            throw new IllegalArgumentException("baseUrl must not include a path");
        }
        return base;
    }

    public static String sanitizeUsername(String rawUsername) {
        if (rawUsername == null || rawUsername.trim().isBlank()) {
            throw new IllegalArgumentException("username is required");
        }
        return rawUsername.trim();
    }

    public static String requirePassword(String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException("password is required");
        }
        return rawPassword;
    }

    public static void validatePort(int port) {
        TargetConnectionValidation.validatePort(port);
    }

    public static void validateTimeouts(int connectTimeoutMs, int requestTimeoutMs) {
        TargetConnectionValidation.validateTimeouts(connectTimeoutMs, requestTimeoutMs);
    }

    public static URI managerHtmlUri(String rawBaseUrl, int port) {
        validatePort(port);
        URI base = parseBaseUrl(rawBaseUrl);
        try {
            return new URI(base.getScheme(), null, base.getHost(), port, "/manager/html", null, null);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid manager URI", e);
        }
    }
}
