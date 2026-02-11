package io.pockethive.hivewatch.service.environments;

import io.pockethive.hivewatch.service.api.EnvironmentCreateRequestDto;
import io.pockethive.hivewatch.service.api.EnvironmentSummaryDto;
import io.pockethive.hivewatch.service.api.EnvironmentUpdateRequestDto;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

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

    @PostMapping("/api/v1/admin/environments")
    public EnvironmentSummaryDto create(@RequestBody EnvironmentCreateRequestDto request) {
        if (request == null) {
            throw new ResponseStatusException(BAD_REQUEST, "Request body is required");
        }
        if (request.name() == null || request.name().trim().isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "name is required");
        }
        String name = request.name().trim();
        if (environmentRepository.findByName(name).isPresent()) {
            throw new ResponseStatusException(BAD_REQUEST, "name already exists");
        }
        EnvironmentEntity saved = environmentRepository.save(new EnvironmentEntity(UUID.randomUUID(), name));
        return new EnvironmentSummaryDto(saved.getId(), saved.getName());
    }

    @PutMapping("/api/v1/admin/environments/{environmentId}")
    public EnvironmentSummaryDto rename(
            @PathVariable("environmentId") UUID environmentId,
            @RequestBody EnvironmentUpdateRequestDto request
    ) {
        if (request == null) {
            throw new ResponseStatusException(BAD_REQUEST, "Request body is required");
        }
        if (request.name() == null || request.name().trim().isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "name is required");
        }
        String name = request.name().trim();

        EnvironmentEntity env = environmentRepository.findById(environmentId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Environment not found"));

        Optional<EnvironmentEntity> clash = environmentRepository.findByName(name);
        if (clash.isPresent() && !clash.get().getId().equals(environmentId)) {
            throw new ResponseStatusException(BAD_REQUEST, "name already exists");
        }

        env.setName(name);
        EnvironmentEntity saved = environmentRepository.save(env);
        return new EnvironmentSummaryDto(saved.getId(), saved.getName());
    }

    @DeleteMapping("/api/v1/admin/environments/{environmentId}")
    public void delete(@PathVariable("environmentId") UUID environmentId) {
        EnvironmentEntity env = environmentRepository.findById(environmentId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Environment not found"));
        environmentRepository.delete(env);
    }
}
