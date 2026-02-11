package io.pockethive.hivewatch.service.api;

import java.util.List;

public record TomcatExpectedWebappsReplaceRequestDto(
        List<TomcatExpectedWebappItemDto> items
) {
}

