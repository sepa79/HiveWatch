package io.pockethive.hivewatch.service.tomcat;

import io.pockethive.hivewatch.service.api.TomcatScanErrorKind;
import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;

final class TomcatManagerHtmlClient {
    TomcatManagerFetchResult fetchWebapps(TomcatTargetEntity target) {
        URI managerUri;
        try {
            managerUri = managerHtmlUri(target);
        } catch (RuntimeException e) {
            return TomcatManagerFetchResult.error(TomcatScanErrorKind.UNKNOWN, e.getMessage());
        }

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(target.getConnectTimeoutMs()))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();

        String basic = Base64.getEncoder().encodeToString(
                (target.getUsername() + ":" + target.getPassword()).getBytes(StandardCharsets.UTF_8)
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(managerUri)
                .timeout(Duration.ofMillis(target.getRequestTimeoutMs()))
                .header("Authorization", "Basic " + basic)
                .GET()
                .build();

        HttpResponse<String> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (java.net.http.HttpTimeoutException e) {
            return TomcatManagerFetchResult.error(TomcatScanErrorKind.TIMEOUT, "Timeout");
        } catch (ConnectException e) {
            return TomcatManagerFetchResult.error(TomcatScanErrorKind.CONNECTIVITY, "Connection failed");
        } catch (IOException e) {
            return TomcatManagerFetchResult.error(TomcatScanErrorKind.CONNECTIVITY, "I/O error");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return TomcatManagerFetchResult.error(TomcatScanErrorKind.UNKNOWN, "Interrupted");
        }

        int code = response.statusCode();
        if (code == 401 || code == 403) {
            return TomcatManagerFetchResult.error(TomcatScanErrorKind.AUTH, "Unauthorized");
        }
        if (code < 200 || code >= 300) {
            return TomcatManagerFetchResult.error(TomcatScanErrorKind.HTTP, "HTTP " + code);
        }

        try {
            List<String> webapps = TomcatManagerHtmlParser.parseWebapps(response.body());
            return TomcatManagerFetchResult.success(webapps);
        } catch (RuntimeException e) {
            return TomcatManagerFetchResult.error(TomcatScanErrorKind.PARSE, e.getMessage());
        }
    }

    private static URI managerHtmlUri(TomcatTargetEntity target) {
        URI base = URI.create(target.getBaseUrl());
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

        try {
            return new URI(base.getScheme(), null, base.getHost(), target.getPort(), "/manager/html", null, null);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid manager URI", e);
        }
    }

    record TomcatManagerFetchResult(
            boolean ok,
            List<String> webapps,
            TomcatScanErrorKind errorKind,
            String errorMessage
    ) {
        static TomcatManagerFetchResult success(List<String> webapps) {
            return new TomcatManagerFetchResult(true, webapps, null, null);
        }

        static TomcatManagerFetchResult error(TomcatScanErrorKind kind, String message) {
            return new TomcatManagerFetchResult(false, List.of(), kind, message);
        }
    }
}
