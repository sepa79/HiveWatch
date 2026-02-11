package io.pockethive.hivewatch.service.docker;

import io.pockethive.hivewatch.service.api.DockerServicesPageDto;
import io.pockethive.hivewatch.service.security.EnvironmentVisibilityService;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DockerServicesController {
    private final DockerServicesQueryService queryService;
    private final EnvironmentVisibilityService environmentVisibilityService;

    public DockerServicesController(DockerServicesQueryService queryService, EnvironmentVisibilityService environmentVisibilityService) {
        this.queryService = queryService;
        this.environmentVisibilityService = environmentVisibilityService;
    }

    @GetMapping("/api/v1/environments/{environmentId}/docker-servers/{serverId}/services")
    public DockerServicesPageDto list(
            @PathVariable("environmentId") UUID environmentId,
            @PathVariable("serverId") UUID serverId,
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "size", required = false, defaultValue = "50") int size
    ) {
        environmentVisibilityService.requireVisible(environmentId);
        return queryService.listServices(environmentId, serverId, q, page, size);
    }
}

