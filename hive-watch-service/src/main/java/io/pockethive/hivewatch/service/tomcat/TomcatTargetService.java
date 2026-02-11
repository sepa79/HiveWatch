package io.pockethive.hivewatch.service.tomcat;

import io.pockethive.hivewatch.service.api.TomcatScanErrorKind;
import io.pockethive.hivewatch.service.api.TomcatScanOutcomeKind;
import io.pockethive.hivewatch.service.api.TomcatTargetCreateRequestDto;
import io.pockethive.hivewatch.service.api.TomcatTargetDto;
import io.pockethive.hivewatch.service.api.TomcatTargetStateDto;
import io.pockethive.hivewatch.service.api.TomcatTargetUpdateRequestDto;
import io.pockethive.hivewatch.service.api.TomcatWebappDto;
import io.pockethive.hivewatch.service.environments.servers.ServerEntity;
import io.pockethive.hivewatch.service.environments.servers.ServerRepository;
import io.pockethive.hivewatch.service.environments.EnvironmentRepository;
import java.net.URI;
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
public class TomcatTargetService {
    private final EnvironmentRepository environmentRepository;
    private final ServerRepository serverRepository;
    private final TomcatTargetRepository tomcatTargetRepository;
    private final TomcatTargetScanStateRepository tomcatTargetScanStateRepository;

    public TomcatTargetService(
            EnvironmentRepository environmentRepository,
            ServerRepository serverRepository,
            TomcatTargetRepository tomcatTargetRepository,
            TomcatTargetScanStateRepository tomcatTargetScanStateRepository
    ) {
        this.environmentRepository = environmentRepository;
        this.serverRepository = serverRepository;
        this.tomcatTargetRepository = tomcatTargetRepository;
        this.tomcatTargetScanStateRepository = tomcatTargetScanStateRepository;
    }

    @Transactional(readOnly = true)
    public List<TomcatTargetDto> listWithState(UUID environmentId) {
        requireEnvironment(environmentId);
        List<ServerEntity> servers = serverRepository.findByEnvironmentId(environmentId);
        if (servers.isEmpty()) {
            return List.of();
        }
        Map<UUID, ServerEntity> serverById = servers.stream().collect(java.util.stream.Collectors.toMap(ServerEntity::getId, Function.identity()));

        List<TomcatTargetEntity> targets = tomcatTargetRepository.findByServerIdIn(servers.stream().map(ServerEntity::getId).toList());
        Map<UUID, TomcatTargetScanStateEntity> states = tomcatTargetScanStateRepository
                .findAllById(targets.stream().map(TomcatTargetEntity::getId).toList())
                .stream()
                .collect(java.util.stream.Collectors.toMap(TomcatTargetScanStateEntity::getTargetId, Function.identity()));

        return targets.stream().map(t -> toDto(t, serverById.get(t.getServerId()), states.get(t.getId()))).toList();
    }

    @Transactional(readOnly = true)
    public UUID environmentIdForTargetOrThrow(UUID targetId) {
        TomcatTargetEntity target = tomcatTargetRepository.findById(targetId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Tomcat target not found"));
        ServerEntity server = serverRepository.findById(target.getServerId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Server not found for target"));
        return server.getEnvironmentId();
    }

    @Transactional
    public TomcatTargetDto create(UUID environmentId, TomcatTargetCreateRequestDto request) {
        requireEnvironment(environmentId);
        validateCreateRequest(request);

        UUID serverId = request.serverId();
        if (!serverRepository.existsByIdAndEnvironmentId(serverId, environmentId)) {
            throw new ResponseStatusException(BAD_REQUEST, "serverId is not part of this environment");
        }
        ServerEntity server = serverRepository.findById(serverId).orElse(null);

        UUID id = UUID.randomUUID();
        TomcatTargetEntity created = tomcatTargetRepository.save(new TomcatTargetEntity(
                id,
                serverId,
                request.role(),
                request.baseUrl().trim(),
                request.port(),
                request.username().trim(),
                request.password(),
                request.connectTimeoutMs(),
                request.requestTimeoutMs(),
                Instant.now()
        ));

        return toDto(created, server, null);
    }

    @Transactional
    public TomcatTargetDto update(UUID environmentId, UUID targetId, TomcatTargetUpdateRequestDto request) {
        requireEnvironment(environmentId);
        validateUpdateRequest(request);

        TomcatTargetEntity existing = tomcatTargetRepository.findById(targetId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Tomcat target not found"));
        ServerEntity existingServer = serverRepository.findById(existing.getServerId())
                .orElseThrow(() -> new IllegalStateException("Server not found for tomcat target: " + existing.getId()));
        if (!existingServer.getEnvironmentId().equals(environmentId)) {
            throw new ResponseStatusException(NOT_FOUND, "Tomcat target not found");
        }

        if (!serverRepository.existsByIdAndEnvironmentId(request.serverId(), environmentId)) {
            throw new ResponseStatusException(BAD_REQUEST, "serverId is not part of this environment");
        }
        ServerEntity server = serverRepository.findById(request.serverId())
                .orElseThrow(() -> new IllegalStateException("Server not found"));

        TomcatTargetEntity updated = tomcatTargetRepository.save(new TomcatTargetEntity(
                existing.getId(),
                request.serverId(),
                request.role(),
                request.baseUrl().trim(),
                request.port(),
                request.username().trim(),
                request.password(),
                request.connectTimeoutMs(),
                request.requestTimeoutMs(),
                existing.getCreatedAt()
        ));
        tomcatTargetScanStateRepository.deleteById(updated.getId());

        return toDto(updated, server, null);
    }

    @Transactional
    public void delete(UUID environmentId, UUID targetId) {
        requireEnvironment(environmentId);
        TomcatTargetEntity existing = tomcatTargetRepository.findById(targetId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Tomcat target not found"));
        ServerEntity server = serverRepository.findById(existing.getServerId())
                .orElseThrow(() -> new IllegalStateException("Server not found for tomcat target: " + existing.getId()));
        if (!server.getEnvironmentId().equals(environmentId)) {
            throw new ResponseStatusException(NOT_FOUND, "Tomcat target not found");
        }
        tomcatTargetRepository.deleteById(targetId);
    }

    private void requireEnvironment(UUID environmentId) {
        if (!environmentRepository.existsById(environmentId)) {
            throw new ResponseStatusException(NOT_FOUND, "Environment not found");
        }
    }

    private static void validateCreateRequest(TomcatTargetCreateRequestDto request) {
        if (request == null) {
            throw new ResponseStatusException(BAD_REQUEST, "Request body is required");
        }
        if (request.serverId() == null) {
            throw new ResponseStatusException(BAD_REQUEST, "serverId is required");
        }
        if (request.role() == null) {
            throw new ResponseStatusException(BAD_REQUEST, "role is required");
        }
        if (request.baseUrl() == null || request.baseUrl().trim().isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "baseUrl is required");
        }
        URI base;
        try {
            base = URI.create(request.baseUrl().trim());
        } catch (RuntimeException e) {
            throw new ResponseStatusException(BAD_REQUEST, "baseUrl is invalid");
        }
        if (!base.isAbsolute() || base.getHost() == null || base.getHost().isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "baseUrl must be absolute and include host");
        }
        if (!"http".equalsIgnoreCase(base.getScheme()) && !"https".equalsIgnoreCase(base.getScheme())) {
            throw new ResponseStatusException(BAD_REQUEST, "baseUrl scheme must be http/https");
        }
        if (base.getUserInfo() != null) {
            throw new ResponseStatusException(BAD_REQUEST, "baseUrl must not include userinfo");
        }
        if (base.getPort() != -1) {
            throw new ResponseStatusException(BAD_REQUEST, "baseUrl must not include port; use explicit port");
        }
        String path = base.getPath();
        if (path != null && !path.isBlank() && !"/".equals(path)) {
            throw new ResponseStatusException(BAD_REQUEST, "baseUrl must not include a path");
        }
        if (request.port() < 1 || request.port() > 65535) {
            throw new ResponseStatusException(BAD_REQUEST, "port must be 1..65535");
        }
        if (request.username() == null || request.username().trim().isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "username is required");
        }
        if (request.password() == null || request.password().isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "password is required");
        }
        if (request.connectTimeoutMs() <= 0) {
            throw new ResponseStatusException(BAD_REQUEST, "connectTimeoutMs must be > 0");
        }
        if (request.requestTimeoutMs() <= 0) {
            throw new ResponseStatusException(BAD_REQUEST, "requestTimeoutMs must be > 0");
        }
    }

    private static void validateUpdateRequest(TomcatTargetUpdateRequestDto request) {
        if (request == null) {
            throw new ResponseStatusException(BAD_REQUEST, "Request body is required");
        }
        if (request.serverId() == null) {
            throw new ResponseStatusException(BAD_REQUEST, "serverId is required");
        }
        if (request.role() == null) {
            throw new ResponseStatusException(BAD_REQUEST, "role is required");
        }
        if (request.baseUrl() == null || request.baseUrl().trim().isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "baseUrl is required");
        }
        URI base;
        try {
            base = URI.create(request.baseUrl().trim());
        } catch (RuntimeException e) {
            throw new ResponseStatusException(BAD_REQUEST, "baseUrl is invalid");
        }
        if (!base.isAbsolute() || base.getHost() == null || base.getHost().isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "baseUrl must be absolute and include host");
        }
        if (!"http".equalsIgnoreCase(base.getScheme()) && !"https".equalsIgnoreCase(base.getScheme())) {
            throw new ResponseStatusException(BAD_REQUEST, "baseUrl scheme must be http/https");
        }
        if (base.getUserInfo() != null) {
            throw new ResponseStatusException(BAD_REQUEST, "baseUrl must not include userinfo");
        }
        if (base.getPort() != -1) {
            throw new ResponseStatusException(BAD_REQUEST, "baseUrl must not include port; use explicit port");
        }
        String path = base.getPath();
        if (path != null && !path.isBlank() && !"/".equals(path)) {
            throw new ResponseStatusException(BAD_REQUEST, "baseUrl must not include a path");
        }
        if (request.port() < 1 || request.port() > 65535) {
            throw new ResponseStatusException(BAD_REQUEST, "port must be 1..65535");
        }
        if (request.username() == null || request.username().trim().isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "username is required");
        }
        if (request.password() == null || request.password().isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "password is required");
        }
        if (request.connectTimeoutMs() <= 0) {
            throw new ResponseStatusException(BAD_REQUEST, "connectTimeoutMs must be > 0");
        }
        if (request.requestTimeoutMs() <= 0) {
            throw new ResponseStatusException(BAD_REQUEST, "requestTimeoutMs must be > 0");
        }
    }

    static TomcatTargetDto toDto(TomcatTargetEntity target, ServerEntity server, TomcatTargetScanStateEntity state) {
        TomcatTargetStateDto stateDto = null;
        if (state != null) {
            stateDto = new TomcatTargetStateDto(
                    state.getScannedAt(),
                    state.getOutcomeKind(),
                    state.getErrorKind(),
                    state.getErrorMessage(),
                    state.getTomcatVersion(),
                    state.getJavaVersion(),
                    state.getOs(),
                    state.getWebapps()
            );
        }
        return new TomcatTargetDto(
                target.getId(),
                target.getServerId(),
                server == null ? "Unknown" : server.getName(),
                target.getRole(),
                target.getBaseUrl(),
                target.getPort(),
                target.getUsername(),
                target.getConnectTimeoutMs(),
                target.getRequestTimeoutMs(),
                stateDto
        );
    }

    static TomcatTargetScanStateEntity successState(
            UUID targetId,
            Instant scannedAt,
            String tomcatVersion,
            String javaVersion,
            String os,
            List<TomcatWebappDto> webapps
    ) {
        return new TomcatTargetScanStateEntity(
                targetId,
                scannedAt,
                TomcatScanOutcomeKind.SUCCESS,
                null,
                null,
                tomcatVersion,
                javaVersion,
                os,
                webapps
        );
    }

    static TomcatTargetScanStateEntity errorState(UUID targetId, Instant scannedAt, TomcatScanErrorKind kind, String message) {
        return new TomcatTargetScanStateEntity(
                targetId,
                scannedAt,
                TomcatScanOutcomeKind.ERROR,
                kind == null ? TomcatScanErrorKind.UNKNOWN : kind,
                truncate(message, 600),
                null,
                null,
                null,
                List.of()
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
