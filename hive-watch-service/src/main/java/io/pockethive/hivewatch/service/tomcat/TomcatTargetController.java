package io.pockethive.hivewatch.service.tomcat;

import io.pockethive.hivewatch.service.api.TomcatTargetCreateRequestDto;
import io.pockethive.hivewatch.service.api.TomcatTargetDto;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TomcatTargetController {
    private final TomcatTargetService tomcatTargetService;
    private final TomcatScanService tomcatScanService;

    public TomcatTargetController(TomcatTargetService tomcatTargetService, TomcatScanService tomcatScanService) {
        this.tomcatTargetService = tomcatTargetService;
        this.tomcatScanService = tomcatScanService;
    }

    @GetMapping("/api/v1/environments/{environmentId}/tomcat-targets")
    public List<TomcatTargetDto> list(@PathVariable("environmentId") UUID environmentId) {
        return tomcatTargetService.listWithState(environmentId);
    }

    @PostMapping("/api/v1/environments/{environmentId}/tomcat-targets")
    public TomcatTargetDto create(
            @PathVariable("environmentId") UUID environmentId,
            @RequestBody TomcatTargetCreateRequestDto request
    ) {
        return tomcatTargetService.create(environmentId, request);
    }

    @PostMapping("/api/v1/environments/{environmentId}/tomcat-targets/scan")
    public List<TomcatTargetDto> scanEnvironment(@PathVariable("environmentId") UUID environmentId) {
        return tomcatScanService.scanEnvironment(environmentId);
    }

    @PostMapping("/api/v1/tomcat-targets/{targetId}/scan")
    public TomcatTargetDto scanTarget(@PathVariable("targetId") UUID targetId) {
        return tomcatScanService.scanTarget(targetId);
    }
}
