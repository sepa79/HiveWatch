package io.pockethive.hivewatch.service.actuator;

import io.pockethive.hivewatch.service.api.ActuatorTargetCreateRequestDto;
import io.pockethive.hivewatch.service.api.ActuatorTargetDto;
import io.pockethive.hivewatch.service.api.ActuatorTargetUpdateRequestDto;
import io.pockethive.hivewatch.service.scans.ScansProperties;
import io.pockethive.hivewatch.service.security.EnvironmentVisibilityService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
public class ActuatorTargetController {
    private final ActuatorTargetService actuatorTargetService;
    private final ActuatorScanService actuatorScanService;
    private final EnvironmentVisibilityService environmentVisibilityService;
    private final ScansProperties scansProperties;

    public ActuatorTargetController(
            ActuatorTargetService actuatorTargetService,
            ActuatorScanService actuatorScanService,
            EnvironmentVisibilityService environmentVisibilityService,
            ScansProperties scansProperties
    ) {
        this.actuatorTargetService = actuatorTargetService;
        this.actuatorScanService = actuatorScanService;
        this.environmentVisibilityService = environmentVisibilityService;
        this.scansProperties = scansProperties;
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
        requireManualScansEnabled();
        environmentVisibilityService.requireVisible(environmentId);
        return actuatorScanService.scanEnvironment(environmentId);
    }

    @PutMapping("/api/v1/environments/{environmentId}/actuator-targets/{targetId}")
    public ActuatorTargetDto update(
            @PathVariable("environmentId") UUID environmentId,
            @PathVariable("targetId") UUID targetId,
            @RequestBody ActuatorTargetUpdateRequestDto request
    ) {
        environmentVisibilityService.requireVisible(environmentId);
        return actuatorTargetService.update(environmentId, targetId, request);
    }

    @DeleteMapping("/api/v1/environments/{environmentId}/actuator-targets/{targetId}")
    public ResponseEntity<Void> delete(
            @PathVariable("environmentId") UUID environmentId,
            @PathVariable("targetId") UUID targetId
    ) {
        environmentVisibilityService.requireVisible(environmentId);
        actuatorTargetService.delete(environmentId, targetId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/v1/actuator-targets/{targetId}/scan")
    public ActuatorTargetDto scanTarget(@PathVariable("targetId") UUID targetId) {
        requireManualScansEnabled();
        UUID environmentId = actuatorTargetService.environmentIdForTargetOrThrow(targetId);
        environmentVisibilityService.requireVisible(environmentId);
        return actuatorScanService.scanTarget(targetId);
    }

    private void requireManualScansEnabled() {
        if (!scansProperties.manualEnabled()) {
            throw new ResponseStatusException(NOT_FOUND, "Not found");
        }
    }
}
