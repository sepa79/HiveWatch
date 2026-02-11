package io.pockethive.hivewatch.service.tomcat.expected;

import io.pockethive.hivewatch.service.api.TomcatExpectedWebappDto;
import io.pockethive.hivewatch.service.api.TomcatExpectedWebappsReplaceRequestDto;
import io.pockethive.hivewatch.service.security.EnvironmentVisibilityService;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TomcatExpectedWebappController {
    private final TomcatExpectedWebappService service;
    private final EnvironmentVisibilityService environmentVisibilityService;

    public TomcatExpectedWebappController(TomcatExpectedWebappService service, EnvironmentVisibilityService environmentVisibilityService) {
        this.service = service;
        this.environmentVisibilityService = environmentVisibilityService;
    }

    @GetMapping("/api/v1/environments/{environmentId}/tomcat-expected-webapps")
    public List<TomcatExpectedWebappDto> list(@PathVariable("environmentId") UUID environmentId) {
        environmentVisibilityService.requireVisible(environmentId);
        return service.list(environmentId);
    }

    @PutMapping("/api/v1/environments/{environmentId}/tomcat-expected-webapps")
    public List<TomcatExpectedWebappDto> replace(
            @PathVariable("environmentId") UUID environmentId,
            @RequestBody TomcatExpectedWebappsReplaceRequestDto request
    ) {
        environmentVisibilityService.requireVisible(environmentId);
        return service.replace(environmentId, request);
    }
}

