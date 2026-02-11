package io.pockethive.hivewatch.service.status;

import io.pockethive.hivewatch.service.api.EnvironmentStatusDto;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EnvironmentStatusController {
    private final EnvironmentStatusQueryService environmentStatusQueryService;

    public EnvironmentStatusController(EnvironmentStatusQueryService environmentStatusQueryService) {
        this.environmentStatusQueryService = environmentStatusQueryService;
    }

    @GetMapping("/api/v1/environments/{environmentId}/status")
    public EnvironmentStatusDto get(@PathVariable("environmentId") UUID environmentId) {
        return environmentStatusQueryService.getStatus(environmentId);
    }
}

