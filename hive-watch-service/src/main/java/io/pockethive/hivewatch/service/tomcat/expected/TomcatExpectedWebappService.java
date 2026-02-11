package io.pockethive.hivewatch.service.tomcat.expected;

import io.pockethive.hivewatch.service.api.TomcatExpectedWebappDto;
import io.pockethive.hivewatch.service.api.TomcatExpectedWebappItemDto;
import io.pockethive.hivewatch.service.api.TomcatExpectedWebappsReplaceRequestDto;
import io.pockethive.hivewatch.service.environments.EnvironmentRepository;
import io.pockethive.hivewatch.service.environments.servers.ServerEntity;
import io.pockethive.hivewatch.service.environments.servers.ServerRepository;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class TomcatExpectedWebappService {
    private final EnvironmentRepository environmentRepository;
    private final ServerRepository serverRepository;
    private final TomcatExpectedWebappRepository repository;

    public TomcatExpectedWebappService(
            EnvironmentRepository environmentRepository,
            ServerRepository serverRepository,
            TomcatExpectedWebappRepository repository
    ) {
        this.environmentRepository = environmentRepository;
        this.serverRepository = serverRepository;
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<TomcatExpectedWebappDto> list(UUID environmentId) {
        requireEnvironment(environmentId);
        List<ServerEntity> servers = serverRepository.findByEnvironmentId(environmentId);
        if (servers.isEmpty()) return List.of();

        List<TomcatExpectedWebappEntity> entities = repository.findByServerIdIn(servers.stream().map(ServerEntity::getId).toList());
        return entities.stream()
                .map(e -> new TomcatExpectedWebappDto(e.getId(), e.getServerId(), e.getRole(), e.getPath(), e.getCreatedAt()))
                .sorted((a, b) -> {
                    int byServer = a.serverId().compareTo(b.serverId());
                    if (byServer != 0) return byServer;
                    int byRole = a.role().compareTo(b.role());
                    if (byRole != 0) return byRole;
                    return a.path().compareToIgnoreCase(b.path());
                })
                .toList();
    }

    @Transactional
    public List<TomcatExpectedWebappDto> replace(UUID environmentId, TomcatExpectedWebappsReplaceRequestDto request) {
        requireEnvironment(environmentId);
        validateReplaceRequest(request);

        List<ServerEntity> servers = serverRepository.findByEnvironmentId(environmentId);
        Map<UUID, ServerEntity> serverById = servers.stream()
                .collect(java.util.stream.Collectors.toMap(ServerEntity::getId, Function.identity()));

        List<TomcatExpectedWebappItemDto> items = request.items() == null ? List.of() : request.items();
        Set<String> dedupe = new HashSet<>();

        for (TomcatExpectedWebappItemDto i : items) {
            if (!serverById.containsKey(i.serverId())) {
                throw new ResponseStatusException(BAD_REQUEST, "serverId is not part of this environment: " + i.serverId());
            }
            if (i.role() == null) {
                throw new ResponseStatusException(BAD_REQUEST, "role is required");
            }
            String path = (i.path() == null ? "" : i.path()).trim();
            if (path.isEmpty()) {
                throw new ResponseStatusException(BAD_REQUEST, "path is required");
            }
            if (!path.startsWith("/")) {
                throw new ResponseStatusException(BAD_REQUEST, "path must start with '/'");
            }
            String key = i.serverId() + "|" + i.role() + "|" + path;
            if (!dedupe.add(key)) {
                throw new ResponseStatusException(BAD_REQUEST, "Duplicate expected webapp entry: " + key);
            }
        }

        List<UUID> serverIds = servers.stream().map(ServerEntity::getId).toList();
        if (!serverIds.isEmpty()) {
            repository.deleteByServerIdIn(serverIds);
        }

        Instant now = Instant.now();
        List<TomcatExpectedWebappEntity> saved = repository.saveAll(items.stream()
                .map(i -> new TomcatExpectedWebappEntity(
                        UUID.randomUUID(),
                        i.serverId(),
                        i.role(),
                        i.path() == null ? "" : i.path().trim(),
                        now
                ))
                .toList());

        return saved.stream()
                .map(e -> new TomcatExpectedWebappDto(e.getId(), e.getServerId(), e.getRole(), e.getPath(), e.getCreatedAt()))
                .toList();
    }

    private void requireEnvironment(UUID environmentId) {
        if (!environmentRepository.existsById(environmentId)) {
            throw new ResponseStatusException(NOT_FOUND, "Environment not found");
        }
    }

    private static void validateReplaceRequest(TomcatExpectedWebappsReplaceRequestDto request) {
        if (request == null) {
            throw new ResponseStatusException(BAD_REQUEST, "Request body is required");
        }
    }
}
