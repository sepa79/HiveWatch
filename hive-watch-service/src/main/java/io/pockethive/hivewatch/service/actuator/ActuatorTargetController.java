package io.pockethive.hivewatch.service.actuator;

import io.pockethive.hivewatch.service.api.ActuatorTargetCreateRequestDto;
import io.pockethive.hivewatch.service.api.ActuatorTargetDto;
import io.pockethive.hivewatch.service.security.EnvironmentVisibilityService;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ActuatorTargetController {
    private final ActuatorTargetService actuatorTargetService;
    private final ActuatorScanService actuatorScanService;
    private final EnvironmentVisibilityService environmentVisibilityService;

    public ActuatorTargetController(
            ActuatorTargetService actuatorTargetService,
            ActuatorScanService actuatorScanService,
            EnvironmentVisibilityService environmentVisibilityService
    ) {
        this.actuatorTargetService = actuatorTargetService;
        this.actuatorScanService = actuatorScanService;
        this.environmentVisibilityService = environmentVisibilityService;
    }

    @GetMapping("/api/v1/environments/{environmentId}/actuator-targets")
    public List<ActuatorTargetDto> list(@PathVariable("environmentId") UUID environmentId) {
        environmentVisibilityService.requireVisible(environmentId);
        return actuatorTargetService.listWithState(environmentId);
    }

    @PostMapping("/api/v1/environments/{environmentId}/actuator-targets")
    public ActuatorTargetDto create(
            @PathVariable("environmentId") UUID environmentId,
            @RequestBody ActuatorTargetCreateRequestDto request
    ) {
        environmentVisibilityService.requireVisible(environmentId);
        return actuatorTargetService.create(environmentId, request);
    }

    @PostMapping("/api/v1/environments/{environmentId}/actuator-targets/scan")
    public List<ActuatorTargetDto> scanEnvironment(@PathVariable("environmentId") UUID environmentId) {
        environmentVisibilityService.requireVisible(environmentId);
        return actuatorScanService.scanEnvironment(environmentId);
    }

    @PostMapping("/api/v1/actuator-targets/{targetId}/scan")
    public ActuatorTargetDto scanTarget(@PathVariable("targetId") UUID targetId) {
        return actuatorScanService.scanTarget(targetId);
    }
}
