package io.pockethive.hivewatch.service.environments.servers;

import io.pockethive.hivewatch.service.api.ServerCreateRequestDto;
import io.pockethive.hivewatch.service.api.ServerDto;
import io.pockethive.hivewatch.service.environments.EnvironmentRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class ServerService {
    private final EnvironmentRepository environmentRepository;
    private final ServerRepository serverRepository;

    public ServerService(EnvironmentRepository environmentRepository, ServerRepository serverRepository) {
        this.environmentRepository = environmentRepository;
        this.serverRepository = serverRepository;
    }

    @Transactional(readOnly = true)
    public List<ServerDto> list(UUID environmentId) {
        requireEnvironment(environmentId);
        return serverRepository.findByEnvironmentId(environmentId).stream().map(ServerService::toDto).toList();
    }

    @Transactional
    public ServerDto create(UUID environmentId, ServerCreateRequestDto request) {
        requireEnvironment(environmentId);
        validateRequest(request);

        ServerEntity created = serverRepository.save(new ServerEntity(
                UUID.randomUUID(),
                environmentId,
                request.name().trim(),
                Instant.now()
        ));
        return toDto(created);
    }

    private void requireEnvironment(UUID environmentId) {
        if (!environmentRepository.existsById(environmentId)) {
            throw new ResponseStatusException(NOT_FOUND, "Environment not found");
        }
    }

    private static void validateRequest(ServerCreateRequestDto request) {
        if (request == null) {
            throw new ResponseStatusException(BAD_REQUEST, "Request body is required");
        }
        if (request.name() == null || request.name().trim().isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "name is required");
        }
    }

    static ServerDto toDto(ServerEntity entity) {
        return new ServerDto(entity.getId(), entity.getEnvironmentId(), entity.getName());
    }
}

