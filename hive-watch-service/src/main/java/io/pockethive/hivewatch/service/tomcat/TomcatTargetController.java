package io.pockethive.hivewatch.service.tomcat;

import io.pockethive.hivewatch.service.api.TomcatTargetCreateRequestDto;
import io.pockethive.hivewatch.service.api.TomcatTargetDto;
import io.pockethive.hivewatch.service.api.TomcatTargetUpdateRequestDto;
import io.pockethive.hivewatch.service.scans.ScansProperties;
import io.pockethive.hivewatch.service.security.EnvironmentVisibilityService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
public class TomcatTargetController {
    private final TomcatTargetService tomcatTargetService;
    private final TomcatScanService tomcatScanService;
    private final EnvironmentVisibilityService environmentVisibilityService;
    private final ScansProperties scansProperties;

    public TomcatTargetController(
            TomcatTargetService tomcatTargetService,
            TomcatScanService tomcatScanService,
            EnvironmentVisibilityService environmentVisibilityService,
            ScansProperties scansProperties
    ) {
        this.tomcatTargetService = tomcatTargetService;
        this.tomcatScanService = tomcatScanService;
        this.environmentVisibilityService = environmentVisibilityService;
        this.scansProperties = scansProperties;
    }

    @GetMapping("/api/v1/environments/{environmentId}/tomcat-targets")
    public List<TomcatTargetDto> list(@PathVariable("environmentId") UUID environmentId) {
        environmentVisibilityService.requireVisible(environmentId);
        return tomcatTargetService.listWithState(environmentId);
    }

    @PostMapping("/api/v1/environments/{environmentId}/tomcat-targets")
    public TomcatTargetDto create(
            @PathVariable("environmentId") UUID environmentId,
            @RequestBody TomcatTargetCreateRequestDto request
    ) {
        environmentVisibilityService.requireVisible(environmentId);
        return tomcatTargetService.create(environmentId, request);
    }

    @PostMapping("/api/v1/environments/{environmentId}/tomcat-targets/scan")
    public List<TomcatTargetDto> scanEnvironment(@PathVariable("environmentId") UUID environmentId) {
        requireManualScansEnabled();
        environmentVisibilityService.requireVisible(environmentId);
        return tomcatScanService.scanEnvironment(environmentId);
    }

    @PutMapping("/api/v1/environments/{environmentId}/tomcat-targets/{targetId}")
    public TomcatTargetDto update(
            @PathVariable("environmentId") UUID environmentId,
            @PathVariable("targetId") UUID targetId,
            @RequestBody TomcatTargetUpdateRequestDto request
    ) {
        environmentVisibilityService.requireVisible(environmentId);
        return tomcatTargetService.update(environmentId, targetId, request);
    }

    @DeleteMapping("/api/v1/environments/{environmentId}/tomcat-targets/{targetId}")
    public ResponseEntity<Void> delete(
            @PathVariable("environmentId") UUID environmentId,
            @PathVariable("targetId") UUID targetId
    ) {
        environmentVisibilityService.requireVisible(environmentId);
        tomcatTargetService.delete(environmentId, targetId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/v1/tomcat-targets/{targetId}/scan")
    public TomcatTargetDto scanTarget(@PathVariable("targetId") UUID targetId) {
        requireManualScansEnabled();
        UUID environmentId = tomcatTargetService.environmentIdForTargetOrThrow(targetId);
        environmentVisibilityService.requireVisible(environmentId);
        return tomcatScanService.scanTarget(targetId);
    }

    private void requireManualScansEnabled() {
        if (!scansProperties.manualEnabled()) {
            throw new ResponseStatusException(NOT_FOUND, "Not found");
        }
    }
}
