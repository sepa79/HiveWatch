package io.pockethive.hivewatch.service.actuator;

import io.pockethive.hivewatch.service.api.ActuatorTargetDto;
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

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class ActuatorScanService {
    private final ServerRepository serverRepository;
    private final ActuatorTargetRepository actuatorTargetRepository;
    private final ActuatorTargetScanStateRepository actuatorTargetScanStateRepository;
    private final ActuatorClient actuatorClient;

    public ActuatorScanService(
            ServerRepository serverRepository,
            ActuatorTargetRepository actuatorTargetRepository,
            ActuatorTargetScanStateRepository actuatorTargetScanStateRepository,
            ActuatorClient actuatorClient
    ) {
        this.serverRepository = serverRepository;
        this.actuatorTargetRepository = actuatorTargetRepository;
        this.actuatorTargetScanStateRepository = actuatorTargetScanStateRepository;
        this.actuatorClient = actuatorClient;
    }

    @Transactional
    public List<ActuatorTargetDto> scanEnvironment(UUID environmentId) {
        List<ServerEntity> servers = serverRepository.findByEnvironmentId(environmentId);
        if (servers.isEmpty()) {
            return List.of();
        }
        Map<UUID, ServerEntity> serverById = servers.stream()
                .collect(java.util.stream.Collectors.toMap(ServerEntity::getId, Function.identity()));

        List<ActuatorTargetEntity> targets = actuatorTargetRepository.findByServerIdIn(
                servers.stream().map(ServerEntity::getId).toList()
        );
        for (ActuatorTargetEntity target : targets) {
            scanInternal(target);
        }

        Map<UUID, ActuatorTargetScanStateEntity> states = actuatorTargetScanStateRepository
                .findAllById(targets.stream().map(ActuatorTargetEntity::getId).toList())
                .stream()
                .collect(java.util.stream.Collectors.toMap(ActuatorTargetScanStateEntity::getTargetId, Function.identity()));

        return targets.stream().map(t -> ActuatorTargetService.toDto(t, serverById.get(t.getServerId()), states.get(t.getId()))).toList();
    }

    @Transactional
    public ActuatorTargetDto scanTarget(UUID targetId) {
        ActuatorTargetEntity target = actuatorTargetRepository.findById(targetId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Actuator target not found"));

        scanInternal(target);

        ActuatorTargetScanStateEntity state = actuatorTargetScanStateRepository.findById(targetId).orElse(null);
        ServerEntity server = serverRepository.findById(target.getServerId()).orElse(null);
        return ActuatorTargetService.toDto(target, server, state);
    }

    private void scanInternal(ActuatorTargetEntity target) {
        Instant now = Instant.now();
        ActuatorClient.ActuatorFetchResult result = actuatorClient.fetch(target);
        ActuatorTargetScanStateEntity state = result.ok()
                ? ActuatorTargetService.successState(
                        target.getId(),
                        now,
                        result.healthStatus(),
                        result.appName(),
                        result.buildVersion(),
                        result.cpuUsage(),
                        result.memoryUsedBytes()
                )
                : ActuatorTargetService.errorState(target.getId(), now, result.errorKind(), result.errorMessage());
        actuatorTargetScanStateRepository.save(state);
    }
}
