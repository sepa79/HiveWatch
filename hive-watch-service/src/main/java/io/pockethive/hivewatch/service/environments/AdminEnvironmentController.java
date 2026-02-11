package io.pockethive.hivewatch.service.environments;

import io.pockethive.hivewatch.service.api.EnvironmentSummaryDto;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminEnvironmentController {
    private final EnvironmentRepository environmentRepository;

    public AdminEnvironmentController(EnvironmentRepository environmentRepository) {
        this.environmentRepository = environmentRepository;
    }

    @GetMapping("/api/v1/admin/environments")
    public List<EnvironmentSummaryDto> listAll() {
        return environmentRepository.findAll(Sort.by(Sort.Direction.ASC, "name")).stream()
                .map(e -> new EnvironmentSummaryDto(e.getId(), e.getName()))
                .toList();
    }
}

