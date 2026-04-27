package io.pockethive.hivewatch.service.targetroles;

import io.pockethive.hivewatch.service.api.EnvironmentTargetRoleDto;
import io.pockethive.hivewatch.service.api.EnvironmentTargetRoleReplaceRequestDto;
import io.pockethive.hivewatch.service.actuator.ActuatorTargetEntity;
import io.pockethive.hivewatch.service.actuator.ActuatorTargetRepository;
import io.pockethive.hivewatch.service.environments.EnvironmentRepository;
import io.pockethive.hivewatch.service.environments.servers.ServerEntity;
import io.pockethive.hivewatch.service.environments.servers.ServerRepository;
import io.pockethive.hivewatch.service.tomcat.TomcatTargetEntity;
import io.pockethive.hivewatch.service.tomcat.TomcatTargetRepository;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class EnvironmentTargetRoleService {
    private final EnvironmentRepository environmentRepository;
    private final EnvironmentTargetRoleRepository repository;
    private final ServerRepository serverRepository;
    private final TomcatTargetRepository tomcatTargetRepository;
    private final ActuatorTargetRepository actuatorTargetRepository;

    public EnvironmentTargetRoleService(
            EnvironmentRepository environmentRepository,
            EnvironmentTargetRoleRepository repository,
            ServerRepository serverRepository,
            TomcatTargetRepository tomcatTargetRepository,
            ActuatorTargetRepository actuatorTargetRepository
    ) {
        this.environmentRepository = environmentRepository;
        this.repository = repository;
        this.serverRepository = serverRepository;
        this.tomcatTargetRepository = tomcatTargetRepository;
        this.actuatorTargetRepository = actuatorTargetRepository;
    }

    @Transactional(readOnly = true)
    public List<EnvironmentTargetRoleDto> list(UUID environmentId) {
        requireEnvironment(environmentId);
        return repository.findByEnvironmentId(environmentId).stream()
                .sorted(roleComparator())
                .map(EnvironmentTargetRoleService::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public void requireActiveRole(UUID environmentId, String rawRole) {
        String role = sanitizeCode(rawRole);
        EnvironmentTargetRoleEntity configured = repository.findByEnvironmentIdAndCode(environmentId, role)
                .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "role is not configured for this environment: " + role));
        if (!configured.isActive()) {
            throw new ResponseStatusException(BAD_REQUEST, "role is inactive for this environment: " + role);
        }
    }

    @Transactional
    public List<EnvironmentTargetRoleDto> replace(UUID environmentId, EnvironmentTargetRoleReplaceRequestDto request) {
        requireEnvironment(environmentId);
        if (request == null) {
            throw new ResponseStatusException(BAD_REQUEST, "Request body is required");
        }
        if (request.roles() == null) {
            throw new ResponseStatusException(BAD_REQUEST, "roles is required");
        }
        if (request.roles().isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "At least one role is required");
        }

        Set<String> seen = new HashSet<>();
        Instant now = Instant.now();
        List<EnvironmentTargetRoleEntity> roles = request.roles().stream()
                .map(dto -> {
                    if (dto == null) {
                        throw new ResponseStatusException(BAD_REQUEST, "roles cannot contain null");
                    }
                    String code = sanitizeCode(dto.code());
                    String label = sanitizeLabel(dto.label());
                    if (!seen.add(code)) {
                        throw new ResponseStatusException(BAD_REQUEST, "Duplicate role code: " + code);
                    }
                    return new EnvironmentTargetRoleEntity(UUID.randomUUID(), environmentId, code, label, dto.sortOrder(), dto.active(), now);
                })
                .toList();

        Set<String> configuredCodes = roles.stream()
                .map(EnvironmentTargetRoleEntity::getCode)
                .collect(java.util.stream.Collectors.toSet());
        for (String usedRole : usedTargetRoles(environmentId)) {
            if (!configuredCodes.contains(usedRole)) {
                throw new ResponseStatusException(BAD_REQUEST, "role is used by existing targets and cannot be removed: " + usedRole);
            }
        }

        repository.deleteByEnvironmentId(environmentId);
        repository.flush();
        repository.saveAll(roles);
        return list(environmentId);
    }

    @Transactional
    public void createDefaults(UUID environmentId) {
        if (!repository.findByEnvironmentId(environmentId).isEmpty()) {
            return;
        }
        Instant now = Instant.now();
        repository.saveAll(List.of(
                new EnvironmentTargetRoleEntity(UUID.randomUUID(), environmentId, "AUTH", "auth", 10, true, now),
                new EnvironmentTargetRoleEntity(UUID.randomUUID(), environmentId, "PAYMENTS", "payments", 20, true, now),
                new EnvironmentTargetRoleEntity(UUID.randomUUID(), environmentId, "SERVICES", "services", 30, true, now)
        ));
    }

    public static String sanitizeCode(String raw) {
        if (raw == null || raw.trim().isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "role is required");
        }
        String code = raw.trim();
        if (code.length() > 64) {
            throw new ResponseStatusException(BAD_REQUEST, "role must be at most 64 characters");
        }
        if (!code.matches("[A-Za-z0-9._:-]+")) {
            throw new ResponseStatusException(BAD_REQUEST, "role may contain only letters, digits, dot, underscore, colon, or hyphen");
        }
        return code;
    }

    private static String sanitizeLabel(String raw) {
        if (raw == null || raw.trim().isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "label is required");
        }
        String label = raw.trim();
        if (label.length() > 120) {
            throw new ResponseStatusException(BAD_REQUEST, "label must be at most 120 characters");
        }
        return label;
    }

    private void requireEnvironment(UUID environmentId) {
        if (!environmentRepository.existsById(environmentId)) {
            throw new ResponseStatusException(NOT_FOUND, "Environment not found");
        }
    }

    private Set<String> usedTargetRoles(UUID environmentId) {
        List<UUID> serverIds = serverRepository.findByEnvironmentId(environmentId).stream()
                .map(ServerEntity::getId)
                .toList();
        if (serverIds.isEmpty()) {
            return Set.of();
        }
        Set<String> roles = new HashSet<>();
        for (TomcatTargetEntity target : tomcatTargetRepository.findByServerIdIn(serverIds)) {
            roles.add(target.getRole());
        }
        for (ActuatorTargetEntity target : actuatorTargetRepository.findByServerIdIn(serverIds)) {
            roles.add(target.getRole());
        }
        return roles;
    }

    private static Comparator<EnvironmentTargetRoleEntity> roleComparator() {
        return Comparator.comparingInt(EnvironmentTargetRoleEntity::getSortOrder)
                .thenComparing(EnvironmentTargetRoleEntity::getLabel, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(EnvironmentTargetRoleEntity::getCode, String.CASE_INSENSITIVE_ORDER);
    }

    private static EnvironmentTargetRoleDto toDto(EnvironmentTargetRoleEntity entity) {
        return new EnvironmentTargetRoleDto(entity.getCode(), entity.getLabel(), entity.getSortOrder(), entity.isActive());
    }
}
