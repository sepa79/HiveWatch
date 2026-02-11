package io.pockethive.hivewatch.service.actuator;

import io.pockethive.hivewatch.service.api.ActuatorTargetCreateRequestDto;
import io.pockethive.hivewatch.service.api.ActuatorTargetDto;
import io.pockethive.hivewatch.service.api.ActuatorTargetStateDto;
import io.pockethive.hivewatch.service.api.TomcatScanErrorKind;
import io.pockethive.hivewatch.service.api.TomcatScanOutcomeKind;
import io.pockethive.hivewatch.service.environments.EnvironmentRepository;
import io.pockethive.hivewatch.service.environments.servers.ServerEntity;
import io.pockethive.hivewatch.service.environments.servers.ServerRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class ActuatorTargetService {
    private final EnvironmentRepository environmentRepository;
    private final ServerRepository serverRepository;
    private final ActuatorTargetRepository actuatorTargetRepository;
    private final ActuatorTargetScanStateRepository actuatorTargetScanStateRepository;

    public ActuatorTargetService(
            EnvironmentRepository environmentRepository,
            ServerRepository serverRepository,
            ActuatorTargetRepository actuatorTargetRepository,
            ActuatorTargetScanStateRepository actuatorTargetScanStateRepository
    ) {
        this.environmentRepository = environmentRepository;
        this.serverRepository = serverRepository;
        this.actuatorTargetRepository = actuatorTargetRepository;
        this.actuatorTargetScanStateRepository = actuatorTargetScanStateRepository;
    }

    @Transactional(readOnly = true)
    public List<ActuatorTargetDto> listWithState(UUID environmentId) {
        requireEnvironment(environmentId);
        List<ServerEntity> servers = serverRepository.findByEnvironmentId(environmentId);
        if (servers.isEmpty()) {
            return List.of();
        }

        Map<UUID, ServerEntity> serverById = servers.stream()
                .collect(java.util.stream.Collectors.toMap(ServerEntity::getId, Function.identity()));

        List<ActuatorTargetEntity> targets = actuatorTargetRepository.findByServerIdIn(
                servers.stream().map(ServerEntity::getId).toList()
        );

        Map<UUID, ActuatorTargetScanStateEntity> states = actuatorTargetScanStateRepository
                .findAllById(targets.stream().map(ActuatorTargetEntity::getId).toList())
                .stream()
                .collect(java.util.stream.Collectors.toMap(ActuatorTargetScanStateEntity::getTargetId, Function.identity()));

        return targets.stream().map(t -> toDto(t, serverById.get(t.getServerId()), states.get(t.getId()))).toList();
    }

    @Transactional
    public ActuatorTargetDto create(UUID environmentId, ActuatorTargetCreateRequestDto request) {
        requireEnvironment(environmentId);
        validateCreateRequest(request);

        UUID serverId = request.serverId();
        if (!serverRepository.existsByIdAndEnvironmentId(serverId, environmentId)) {
            throw new ResponseStatusException(BAD_REQUEST, "serverId is not part of this environment");
        }
        ServerEntity server = serverRepository.findById(serverId).orElse(null);

        ActuatorTargetEntity created = actuatorTargetRepository.save(new ActuatorTargetEntity(
                UUID.randomUUID(),
                serverId,
                request.role(),
                request.baseUrl().trim(),
                request.port(),
                request.profile().trim(),
                request.connectTimeoutMs(),
                request.requestTimeoutMs(),
                Instant.now()
        ));
        return toDto(created, server, null);
    }

    private void requireEnvironment(UUID environmentId) {
        if (!environmentRepository.existsById(environmentId)) {
            throw new ResponseStatusException(NOT_FOUND, "Environment not found");
        }
    }

    private static void validateCreateRequest(ActuatorTargetCreateRequestDto request) {
        if (request == null) {
            throw new ResponseStatusException(BAD_REQUEST, "Request body is required");
        }
        if (request.serverId() == null) {
            throw new ResponseStatusException(BAD_REQUEST, "serverId is required");
        }
        if (request.role() == null) {
            throw new ResponseStatusException(BAD_REQUEST, "role is required");
        }
        if (request.port() < 1 || request.port() > 65535) {
            throw new ResponseStatusException(BAD_REQUEST, "port must be 1..65535");
        }
        if (request.connectTimeoutMs() <= 0) {
            throw new ResponseStatusException(BAD_REQUEST, "connectTimeoutMs must be > 0");
        }
        if (request.requestTimeoutMs() <= 0) {
            throw new ResponseStatusException(BAD_REQUEST, "requestTimeoutMs must be > 0");
        }

        try {
            ActuatorTargetValidation.parseBaseUrl(request.baseUrl());
            ActuatorTargetValidation.sanitizeProfile(request.profile());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(BAD_REQUEST, e.getMessage());
        }
    }

    static ActuatorTargetDto toDto(ActuatorTargetEntity target, ServerEntity server, ActuatorTargetScanStateEntity state) {
        if (server == null) {
            throw new IllegalStateException("Server not found for actuator target: " + target.getId());
        }
        ActuatorTargetStateDto stateDto = null;
        if (state != null) {
            stateDto = new ActuatorTargetStateDto(
                    state.getScannedAt(),
                    state.getOutcomeKind(),
                    state.getErrorKind(),
                    state.getErrorMessage(),
                    state.getHealthStatus(),
                    state.getAppName(),
                    state.getCpuUsage(),
                    state.getMemoryUsedBytes()
            );
        }

        return new ActuatorTargetDto(
                target.getId(),
                target.getServerId(),
                server.getName(),
                target.getRole(),
                target.getBaseUrl(),
                target.getPort(),
                target.getProfile(),
                stateDto
        );
    }

    static ActuatorTargetScanStateEntity successState(
            UUID targetId,
            Instant scannedAt,
            String healthStatus,
            String appName,
            Double cpuUsage,
            Long memoryUsedBytes
    ) {
        return new ActuatorTargetScanStateEntity(
                targetId,
                scannedAt,
                TomcatScanOutcomeKind.SUCCESS,
                null,
                null,
                healthStatus,
                appName,
                cpuUsage,
                memoryUsedBytes
        );
    }

    static ActuatorTargetScanStateEntity errorState(UUID targetId, Instant scannedAt, TomcatScanErrorKind kind, String message) {
        return new ActuatorTargetScanStateEntity(
                targetId,
                scannedAt,
                TomcatScanOutcomeKind.ERROR,
                kind == null ? TomcatScanErrorKind.UNKNOWN : kind,
                truncate(message, 600),
                null,
                null,
                null,
                null
        );
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        String trimmed = s.trim();
        if (trimmed.length() <= max) {
            return trimmed;
        }
        return trimmed.substring(0, Math.max(0, max - 1)) + "…";
    }
}
