package io.pockethive.hivewatch.service.api;

public record ApiErrorDto(
        int status,
        String error,
        String message,
        String path
) {
}
