package io.pockethive.hivewatch.service.api;

public record TargetProbeResultDto(
        TargetAdapterTypeDto adapterType,
        TomcatScanOutcomeKind outcomeKind,
        TomcatScanErrorKind errorKind,
        String errorMessage,
        TargetProbeObservedDto observed,
        TargetProbeCandidateDto candidate
) {
}
