package io.pockethive.hivewatch.service.api;

import java.util.List;

public record TomcatManagerHtmlProbeObservedDto(
        TargetAdapterTypeDto adapterType,
        String tomcatVersion,
        String javaVersion,
        String os,
        List<TomcatWebappDto> webapps
) implements TargetProbeObservedDto {
}
