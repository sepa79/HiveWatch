package io.pockethive.hivewatch.service.actuator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.pockethive.hivewatch.service.api.TomcatScanErrorKind;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
public class ActuatorClient {
    private final ObjectMapper objectMapper;

    public ActuatorClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    ActuatorFetchResult fetch(ActuatorTargetEntity target) {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(target.getConnectTimeoutMs()))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();

        try {
            URI healthUri = endpointUri(target, "/actuator/health");
            URI infoUri = endpointUri(target, "/actuator/info");
            URI cpuUri = endpointUri(target, "/actuator/metrics/system.cpu.usage");
            URI memUri = endpointUri(target, "/actuator/metrics/jvm.memory.used");

            JsonNode health = getJson(client, healthUri, target.getRequestTimeoutMs());
            String healthStatus = textOrThrow(health, "status");

            JsonNode info = getJson(client, infoUri, target.getRequestTimeoutMs());
            String appName = textOrThrow(info.path("app"), "name");
            String buildVersion = textOrNull(info.path("app").path("build"), "version");

            JsonNode cpu = getJson(client, cpuUri, target.getRequestTimeoutMs());
            double cpuUsage = metricValueAsDoubleOrThrow(cpu);

            JsonNode mem = getJson(client, memUri, target.getRequestTimeoutMs());
            long memoryUsedBytes = metricValueAsLongOrThrow(mem);

            return ActuatorFetchResult.success(healthStatus, appName, buildVersion, cpuUsage, memoryUsedBytes);
        } catch (IllegalArgumentException e) {
            return ActuatorFetchResult.error(TomcatScanErrorKind.UNKNOWN, e.getMessage());
        } catch (ActuatorFetchException e) {
            return ActuatorFetchResult.error(e.kind, e.getMessage());
        }
    }

    private static URI endpointUri(ActuatorTargetEntity target, String suffixPath) {
        try {
            return ActuatorTargetValidation.endpointUri(
                    target.getBaseUrl(),
                    target.getPort(),
                    target.getProfile(),
                    suffixPath
            );
        } catch (IllegalArgumentException e) {
            throw new ActuatorFetchException(TomcatScanErrorKind.UNKNOWN, e.getMessage());
        }
    }

    private JsonNode getJson(HttpClient client, URI uri, int requestTimeoutMs) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(Duration.ofMillis(requestTimeoutMs))
                .GET()
                .build();
        HttpResponse<String> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (java.net.http.HttpTimeoutException e) {
            throw new ActuatorFetchException(TomcatScanErrorKind.TIMEOUT, "Timeout");
        } catch (ConnectException e) {
            throw new ActuatorFetchException(TomcatScanErrorKind.CONNECTIVITY, "Connection failed");
        } catch (IOException e) {
            throw new ActuatorFetchException(TomcatScanErrorKind.CONNECTIVITY, "I/O error");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ActuatorFetchException(TomcatScanErrorKind.UNKNOWN, "Interrupted");
        }

        int code = response.statusCode();
        if (code == 401 || code == 403) {
            throw new ActuatorFetchException(TomcatScanErrorKind.AUTH, "Unauthorized");
        }
        if (code < 200 || code >= 300) {
            throw new ActuatorFetchException(TomcatScanErrorKind.HTTP, "HTTP " + code);
        }

        try {
            return objectMapper.readTree(response.body());
        } catch (IOException e) {
            throw new ActuatorFetchException(TomcatScanErrorKind.PARSE, "Invalid JSON");
        }
    }

    private static String textOrThrow(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.isTextual() || value.asText().isBlank()) {
            throw new ActuatorFetchException(TomcatScanErrorKind.PARSE, "Missing field: " + field);
        }
        return value.asText();
    }

    private static String textOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.isTextual()) {
            return null;
        }
        String s = value.asText();
        if (s == null) {
            return null;
        }
        String trimmed = s.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    private static double metricValueAsDoubleOrThrow(JsonNode metric) {
        JsonNode measurements = metric.get("measurements");
        if (measurements == null || !measurements.isArray() || measurements.isEmpty()) {
            throw new ActuatorFetchException(TomcatScanErrorKind.PARSE, "Missing measurements");
        }
        JsonNode first = measurements.get(0);
        if (first == null) {
            throw new ActuatorFetchException(TomcatScanErrorKind.PARSE, "Missing measurement[0]");
        }
        JsonNode value = first.get("value");
        if (value == null || !value.isNumber()) {
            throw new ActuatorFetchException(TomcatScanErrorKind.PARSE, "Missing measurement value");
        }
        return value.asDouble();
    }

    private static long metricValueAsLongOrThrow(JsonNode metric) {
        JsonNode measurements = metric.get("measurements");
        if (measurements == null || !measurements.isArray() || measurements.isEmpty()) {
            throw new ActuatorFetchException(TomcatScanErrorKind.PARSE, "Missing measurements");
        }
        JsonNode first = measurements.get(0);
        if (first == null) {
            throw new ActuatorFetchException(TomcatScanErrorKind.PARSE, "Missing measurement[0]");
        }
        JsonNode value = first.get("value");
        if (value == null || !value.isNumber()) {
            throw new ActuatorFetchException(TomcatScanErrorKind.PARSE, "Missing measurement value");
        }
        return value.asLong();
    }

    private static final class ActuatorFetchException extends RuntimeException {
        final TomcatScanErrorKind kind;

        ActuatorFetchException(TomcatScanErrorKind kind, String message) {
            super(message);
            this.kind = kind;
        }
    }

    record ActuatorFetchResult(
            boolean ok,
            String healthStatus,
            String appName,
            String buildVersion,
            Double cpuUsage,
            Long memoryUsedBytes,
            TomcatScanErrorKind errorKind,
            String errorMessage
    ) {
        static ActuatorFetchResult success(
                String healthStatus,
                String appName,
                String buildVersion,
                double cpuUsage,
                long memoryUsedBytes
        ) {
            return new ActuatorFetchResult(true, healthStatus, appName, buildVersion, cpuUsage, memoryUsedBytes, null, null);
        }

        static ActuatorFetchResult error(TomcatScanErrorKind kind, String message) {
            return new ActuatorFetchResult(false, null, null, null, null, null, kind, message);
        }
    }
}
