package io.pockethive.hivewatch.service.actuator;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Pattern;

final class ActuatorTargetValidation {
    private static final Pattern PROFILE_PATTERN = Pattern.compile("[a-zA-Z0-9_-]+");

    private ActuatorTargetValidation() {
    }

    static URI parseBaseUrl(String rawBaseUrl) {
        if (rawBaseUrl == null || rawBaseUrl.trim().isBlank()) {
            throw new IllegalArgumentException("baseUrl is required");
        }
        URI base;
        try {
            base = URI.create(rawBaseUrl.trim());
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("baseUrl is invalid");
        }

        if (!base.isAbsolute()) {
            throw new IllegalArgumentException("baseUrl must be absolute");
        }
        if (!"http".equalsIgnoreCase(base.getScheme()) && !"https".equalsIgnoreCase(base.getScheme())) {
            throw new IllegalArgumentException("baseUrl scheme must be http/https");
        }
        if (base.getUserInfo() != null) {
            throw new IllegalArgumentException("baseUrl must not include userinfo");
        }
        if (base.getHost() == null || base.getHost().isBlank()) {
            throw new IllegalArgumentException("baseUrl must include host");
        }
        if (base.getPort() != -1) {
            throw new IllegalArgumentException("baseUrl must not include port; use explicit port field");
        }
        String path = base.getPath();
        if (path != null && !path.isBlank() && !"/".equals(path)) {
            throw new IllegalArgumentException("baseUrl must not include a path");
        }

        return base;
    }

    static String sanitizeProfile(String rawProfile) {
        if (rawProfile == null || rawProfile.trim().isBlank()) {
            throw new IllegalArgumentException("profile is required");
        }
        String trimmed = rawProfile.trim();
        if (!PROFILE_PATTERN.matcher(trimmed).matches()) {
            throw new IllegalArgumentException("profile must match [a-zA-Z0-9_-]+");
        }
        return trimmed;
    }

    static URI endpointUri(String rawBaseUrl, int port, String rawProfile, String suffixPath) {
        if (suffixPath == null || suffixPath.isBlank() || suffixPath.charAt(0) != '/') {
            throw new IllegalArgumentException("suffixPath must start with '/'");
        }
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("port must be 1..65535");
        }

        URI base = parseBaseUrl(rawBaseUrl);
        String profile = sanitizeProfile(rawProfile);
        String fullPath = "/" + profile + suffixPath;

        try {
            return new URI(base.getScheme(), null, base.getHost(), port, fullPath, null, null);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid actuator URI");
        }
    }
}

