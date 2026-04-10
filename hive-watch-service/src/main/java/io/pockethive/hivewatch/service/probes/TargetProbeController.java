package io.pockethive.hivewatch.service.probes;

import io.pockethive.hivewatch.service.api.TargetProbeRequestDto;
import io.pockethive.hivewatch.service.api.TargetProbeResultDto;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TargetProbeController {
    private final TargetProbeService targetProbeService;

    public TargetProbeController(TargetProbeService targetProbeService) {
        this.targetProbeService = targetProbeService;
    }

    @PostMapping("/api/v1/admin/target-probes")
    public TargetProbeResultDto probe(@RequestBody TargetProbeRequestDto request) {
        return targetProbeService.probe(request);
    }
}
