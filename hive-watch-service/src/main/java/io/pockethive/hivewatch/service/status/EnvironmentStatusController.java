package io.pockethive.hivewatch.service.status;

import io.pockethive.hivewatch.service.api.EnvironmentStatusDto;
import io.pockethive.hivewatch.service.security.EnvironmentVisibilityService;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EnvironmentStatusController {
    private final EnvironmentStatusQueryService environmentStatusQueryService;
    private final EnvironmentVisibilityService environmentVisibilityService;

    public EnvironmentStatusController(
            EnvironmentStatusQueryService environmentStatusQueryService,
            EnvironmentVisibilityService environmentVisibilityService
    ) {
        this.environmentStatusQueryService = environmentStatusQueryService;
        this.environmentVisibilityService = environmentVisibilityService;
    }

    @GetMapping("/api/v1/environments/{environmentId}/status")
    public EnvironmentStatusDto get(@PathVariable("environmentId") UUID environmentId) {
        environmentVisibilityService.requireVisible(environmentId);
        return environmentStatusQueryService.getStatus(environmentId);
    }
}
