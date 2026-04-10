package io.pockethive.hivewatch.service.targets;

public final class TargetConnectionValidation {
    private TargetConnectionValidation() {
    }

    public static void validatePort(int port) {
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("port must be 1..65535");
        }
    }

    public static void validateTimeouts(int connectTimeoutMs, int requestTimeoutMs) {
        if (connectTimeoutMs <= 0) {
            throw new IllegalArgumentException("connectTimeoutMs must be > 0");
        }
        if (requestTimeoutMs <= 0) {
            throw new IllegalArgumentException("requestTimeoutMs must be > 0");
        }
    }
}
