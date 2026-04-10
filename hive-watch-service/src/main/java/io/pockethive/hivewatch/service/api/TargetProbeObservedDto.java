package io.pockethive.hivewatch.service.api;

public sealed interface TargetProbeObservedDto permits TomcatManagerHtmlProbeObservedDto, ActuatorHttpProbeObservedDto {
    TargetAdapterTypeDto adapterType();
}
