package io.pockethive.hivewatch.service.environments;

import io.pockethive.hivewatch.service.api.EnvironmentSummaryDto;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class EnvironmentQueryService {
    private final EnvironmentRepository environmentRepository;

    public EnvironmentQueryService(EnvironmentRepository environmentRepository) {
        this.environmentRepository = environmentRepository;
    }

    public List<EnvironmentSummaryDto> list() {
        return environmentRepository.findAll().stream()
            .map(env -> new EnvironmentSummaryDto(env.getId(), env.getName()))
            .toList();
    }
}

