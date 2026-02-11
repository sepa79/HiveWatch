package io.pockethive.hivewatch.service.security;

import io.pockethive.hivewatch.service.api.HiveWatchRole;
import io.pockethive.hivewatch.service.environments.EnvironmentEntity;
import io.pockethive.hivewatch.service.environments.EnvironmentRepository;
import io.pockethive.hivewatch.service.users.UserEnvironmentVisibilityRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class EnvironmentVisibilityService {
    private final CurrentUserService currentUserService;
    private final EnvironmentRepository environmentRepository;
    private final UserEnvironmentVisibilityRepository userEnvironmentVisibilityRepository;

    public EnvironmentVisibilityService(
            CurrentUserService currentUserService,
            EnvironmentRepository environmentRepository,
            UserEnvironmentVisibilityRepository userEnvironmentVisibilityRepository
    ) {
        this.currentUserService = currentUserService;
        this.environmentRepository = environmentRepository;
        this.userEnvironmentVisibilityRepository = userEnvironmentVisibilityRepository;
    }

    @Transactional(readOnly = true)
    public List<EnvironmentEntity> listVisibleEnvironments() {
        HiveWatchPrincipal principal = currentUserService.requirePrincipal();
        if (principal.roles().contains(HiveWatchRole.ADMIN)) {
            return environmentRepository.findAll(Sort.by(Sort.Direction.ASC, "name"));
        }
        List<UUID> visibleIds = userEnvironmentVisibilityRepository.findByUserId(principal.userId()).stream()
                .map(v -> v.getEnvironmentId())
                .distinct()
                .toList();
        if (visibleIds.isEmpty()) {
            return List.of();
        }
        return environmentRepository.findAllById(visibleIds).stream()
                .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
                .toList();
    }

    @Transactional(readOnly = true)
    public void requireVisible(UUID environmentId) {
        HiveWatchPrincipal principal = currentUserService.requirePrincipal();
        if (principal.roles().contains(HiveWatchRole.ADMIN)) {
            if (!environmentRepository.existsById(environmentId)) {
                throw new ResponseStatusException(NOT_FOUND, "Environment not found");
            }
            return;
        }
        boolean visible = userEnvironmentVisibilityRepository.findByUserId(principal.userId()).stream()
                .anyMatch(v -> v.getEnvironmentId().equals(environmentId));
        if (!visible) {
            throw new ResponseStatusException(NOT_FOUND, "Environment not found");
        }
    }
}

