package io.pockethive.hivewatch.service.environments;

import io.pockethive.hivewatch.service.api.EnvironmentSummaryDto;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EnvironmentController {
    private final EnvironmentQueryService environmentQueryService;

    public EnvironmentController(EnvironmentQueryService environmentQueryService) {
        this.environmentQueryService = environmentQueryService;
    }

    @GetMapping("/api/v1/environments")
    public List<EnvironmentSummaryDto> list() {
        return environmentQueryService.list();
    }
}

