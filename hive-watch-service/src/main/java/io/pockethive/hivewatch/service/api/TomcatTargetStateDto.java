package io.pockethive.hivewatch.service.api;

import java.time.Instant;
import java.util.List;

public record TomcatTargetStateDto(
        Instant scannedAt,
        TomcatScanOutcomeKind outcomeKind,
        TomcatScanErrorKind errorKind,
        String errorMessage,
        List<String> webapps
) {
}

