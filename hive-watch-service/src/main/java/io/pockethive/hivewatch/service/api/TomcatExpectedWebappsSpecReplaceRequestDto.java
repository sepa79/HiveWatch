package io.pockethive.hivewatch.service.api;

import java.util.List;

public record TomcatExpectedWebappsSpecReplaceRequestDto(
        List<TomcatExpectedWebappsSpecDto> specs
) {
}

