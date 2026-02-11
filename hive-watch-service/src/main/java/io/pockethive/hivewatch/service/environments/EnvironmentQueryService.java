package io.pockethive.hivewatch.service.environments;

import io.pockethive.hivewatch.service.api.EnvironmentSummaryDto;
import io.pockethive.hivewatch.service.security.EnvironmentVisibilityService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class EnvironmentQueryService {
    private final EnvironmentVisibilityService environmentVisibilityService;

    public EnvironmentQueryService(EnvironmentVisibilityService environmentVisibilityService) {
        this.environmentVisibilityService = environmentVisibilityService;
    }

    public List<EnvironmentSummaryDto> list() {
        return environmentVisibilityService.listVisibleEnvironments().stream()
            .map(env -> new EnvironmentSummaryDto(env.getId(), env.getName()))
            .toList();
    }
}
