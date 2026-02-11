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
public class ServerExpectedSetsController {
    private final TomcatExpectedWebappsSpecService tomcatExpectedWebappsSpecService;
    private final DockerExpectedServicesSpecService dockerExpectedServicesSpecService;
    private final EnvironmentVisibilityService environmentVisibilityService;

    public ServerExpectedSetsController(
            TomcatExpectedWebappsSpecService tomcatExpectedWebappsSpecService,
            DockerExpectedServicesSpecService dockerExpectedServicesSpecService,
            EnvironmentVisibilityService environmentVisibilityService
    ) {
        this.tomcatExpectedWebappsSpecService = tomcatExpectedWebappsSpecService;
        this.dockerExpectedServicesSpecService = dockerExpectedServicesSpecService;
        this.environmentVisibilityService = environmentVisibilityService;
    }

    @GetMapping("/api/v1/environments/{environmentId}/servers/{serverId}/expected/tomcat-webapps")
    public List<TomcatExpectedWebappsSpecDto> listTomcat(
            @PathVariable("environmentId") UUID environmentId,
            @PathVariable("serverId") UUID serverId
    ) {
        environmentVisibilityService.requireVisible(environmentId);
        return tomcatExpectedWebappsSpecService.listForServer(environmentId, serverId);
    }

    @PutMapping("/api/v1/environments/{environmentId}/servers/{serverId}/expected/tomcat-webapps")
    public List<TomcatExpectedWebappsSpecDto> replaceTomcat(
            @PathVariable("environmentId") UUID environmentId,
            @PathVariable("serverId") UUID serverId,
            @RequestBody TomcatExpectedWebappsSpecReplaceRequestDto request
    ) {
        environmentVisibilityService.requireVisible(environmentId);
        return tomcatExpectedWebappsSpecService.replaceForServer(environmentId, serverId, request);
    }

    @GetMapping("/api/v1/environments/{environmentId}/servers/{serverId}/expected/docker-services")
    public DockerExpectedServicesSpecDto getDocker(
            @PathVariable("environmentId") UUID environmentId,
            @PathVariable("serverId") UUID serverId
    ) {
        environmentVisibilityService.requireVisible(environmentId);
        return dockerExpectedServicesSpecService.getForServer(environmentId, serverId);
    }

    @PutMapping("/api/v1/environments/{environmentId}/servers/{serverId}/expected/docker-services")
    public DockerExpectedServicesSpecDto replaceDocker(
            @PathVariable("environmentId") UUID environmentId,
            @PathVariable("serverId") UUID serverId,
            @RequestBody DockerExpectedServicesSpecReplaceRequestDto request
    ) {
        environmentVisibilityService.requireVisible(environmentId);
        return dockerExpectedServicesSpecService.replaceForServer(environmentId, serverId, request);
    }
}

