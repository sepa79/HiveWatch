package io.pockethive.hivewatch.service.expectedsets;

import io.pockethive.hivewatch.service.api.DockerExpectedServicesSpecDto;
import io.pockethive.hivewatch.service.api.DockerExpectedServicesSpecReplaceRequestDto;
import io.pockethive.hivewatch.service.api.TomcatExpectedWebappsSpecDto;
import io.pockethive.hivewatch.service.api.TomcatExpectedWebappsSpecReplaceRequestDto;
import io.pockethive.hivewatch.service.security.EnvironmentVisibilityService;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ExpectedSetsController {
    private final TomcatExpectedWebappsSpecService tomcatExpectedWebappsSpecService;
    private final DockerExpectedServicesSpecService dockerExpectedServicesSpecService;
    private final EnvironmentVisibilityService environmentVisibilityService;

    public ExpectedSetsController(
            TomcatExpectedWebappsSpecService tomcatExpectedWebappsSpecService,
            DockerExpectedServicesSpecService dockerExpectedServicesSpecService,
            EnvironmentVisibilityService environmentVisibilityService
    ) {
        this.tomcatExpectedWebappsSpecService = tomcatExpectedWebappsSpecService;
        this.dockerExpectedServicesSpecService = dockerExpectedServicesSpecService;
        this.environmentVisibilityService = environmentVisibilityService;
    }

    @GetMapping("/api/v1/environments/{environmentId}/expected/tomcat-webapps")
    public List<TomcatExpectedWebappsSpecDto> listTomcat(@PathVariable("environmentId") UUID environmentId) {
        environmentVisibilityService.requireVisible(environmentId);
        return tomcatExpectedWebappsSpecService.list(environmentId);
    }

    @PutMapping("/api/v1/environments/{environmentId}/expected/tomcat-webapps")
    public List<TomcatExpectedWebappsSpecDto> replaceTomcat(
            @PathVariable("environmentId") UUID environmentId,
            @RequestBody TomcatExpectedWebappsSpecReplaceRequestDto request
    ) {
        environmentVisibilityService.requireVisible(environmentId);
        return tomcatExpectedWebappsSpecService.replace(environmentId, request);
    }

    @GetMapping("/api/v1/environments/{environmentId}/expected/docker-services")
    public List<DockerExpectedServicesSpecDto> listDocker(@PathVariable("environmentId") UUID environmentId) {
        environmentVisibilityService.requireVisible(environmentId);
        return dockerExpectedServicesSpecService.list(environmentId);
    }

    @PutMapping("/api/v1/environments/{environmentId}/expected/docker-services")
    public List<DockerExpectedServicesSpecDto> replaceDocker(
            @PathVariable("environmentId") UUID environmentId,
            @RequestBody DockerExpectedServicesSpecReplaceRequestDto request
    ) {
        environmentVisibilityService.requireVisible(environmentId);
        return dockerExpectedServicesSpecService.replace(environmentId, request);
    }
}

