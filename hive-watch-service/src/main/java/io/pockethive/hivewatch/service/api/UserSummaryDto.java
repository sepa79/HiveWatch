package io.pockethive.hivewatch.service.api;

import java.util.List;
import java.util.UUID;

public record UserSummaryDto(
    UUID id,
    String username,
    String displayName,
    List<HiveWatchRole> roles,
    boolean active
) {
}
