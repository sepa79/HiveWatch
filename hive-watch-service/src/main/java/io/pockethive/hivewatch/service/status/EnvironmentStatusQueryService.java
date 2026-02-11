package io.pockethive.hivewatch.service.status;

import io.pockethive.hivewatch.service.actuator.ActuatorTargetEntity;
import io.pockethive.hivewatch.service.actuator.ActuatorTargetRepository;
import io.pockethive.hivewatch.service.actuator.ActuatorTargetScanStateEntity;
import io.pockethive.hivewatch.service.actuator.ActuatorTargetScanStateRepository;
import io.pockethive.hivewatch.service.api.EnvironmentStatusDto;
import io.pockethive.hivewatch.service.decision.DecisionEngine;
import io.pockethive.hivewatch.service.decision.DecisionEvaluation;
import io.pockethive.hivewatch.service.decision.DecisionInputs;
import io.pockethive.hivewatch.service.environments.EnvironmentEntity;
import io.pockethive.hivewatch.service.environments.EnvironmentRepository;
import io.pockethive.hivewatch.service.environments.servers.ServerEntity;
import io.pockethive.hivewatch.service.environments.servers.ServerRepository;
import io.pockethive.hivewatch.service.tomcat.TomcatTargetEntity;
import io.pockethive.hivewatch.service.tomcat.TomcatTargetRepository;
import io.pockethive.hivewatch.service.tomcat.TomcatTargetScanStateEntity;
import io.pockethive.hivewatch.service.tomcat.TomcatTargetScanStateRepository;
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
public class EnvironmentStatusQueryService {
    private static final int MAX_ISSUES = 50;

    private final EnvironmentRepository environmentRepository;
    private final ServerRepository serverRepository;
    private final TomcatTargetRepository tomcatTargetRepository;
    private final TomcatTargetScanStateRepository tomcatTargetScanStateRepository;
    private final ActuatorTargetRepository actuatorTargetRepository;
    private final ActuatorTargetScanStateRepository actuatorTargetScanStateRepository;
    private final DecisionEngine decisionEngine;

    public EnvironmentStatusQueryService(
            EnvironmentRepository environmentRepository,
            ServerRepository serverRepository,
            TomcatTargetRepository tomcatTargetRepository,
            TomcatTargetScanStateRepository tomcatTargetScanStateRepository,
            ActuatorTargetRepository actuatorTargetRepository,
            ActuatorTargetScanStateRepository actuatorTargetScanStateRepository,
            DecisionEngine decisionEngine
    ) {
        this.environmentRepository = environmentRepository;
        this.serverRepository = serverRepository;
        this.tomcatTargetRepository = tomcatTargetRepository;
        this.tomcatTargetScanStateRepository = tomcatTargetScanStateRepository;
        this.actuatorTargetRepository = actuatorTargetRepository;
        this.actuatorTargetScanStateRepository = actuatorTargetScanStateRepository;
        this.decisionEngine = decisionEngine;
    }

    @Transactional(readOnly = true)
    public EnvironmentStatusDto getStatus(UUID environmentId) {
        EnvironmentEntity env = environmentRepository.findById(environmentId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Environment not found"));

        List<ServerEntity> servers = serverRepository.findByEnvironmentId(environmentId);
        Map<UUID, ServerEntity> serverById = servers.stream()
                .collect(java.util.stream.Collectors.toMap(ServerEntity::getId, Function.identity()));

        List<UUID> serverIds = servers.stream().map(ServerEntity::getId).toList();

        List<TomcatTargetEntity> tomcatTargets = serverIds.isEmpty() ? List.of() : tomcatTargetRepository.findByServerIdIn(serverIds);
        Map<UUID, TomcatTargetScanStateEntity> tomcatStateById = tomcatTargetScanStateRepository
                .findAllById(tomcatTargets.stream().map(TomcatTargetEntity::getId).toList())
                .stream()
                .collect(java.util.stream.Collectors.toMap(TomcatTargetScanStateEntity::getTargetId, Function.identity()));

        List<ActuatorTargetEntity> actuatorTargets = serverIds.isEmpty() ? List.of() : actuatorTargetRepository.findByServerIdIn(serverIds);
        Map<UUID, ActuatorTargetScanStateEntity> actuatorStateById = actuatorTargetScanStateRepository
                .findAllById(actuatorTargets.stream().map(ActuatorTargetEntity::getId).toList())
                .stream()
                .collect(java.util.stream.Collectors.toMap(ActuatorTargetScanStateEntity::getTargetId, Function.identity()));

        List<DecisionInputs.TomcatTargetObservation> tomcatObs = tomcatTargets.stream().map(t -> {
            ServerEntity server = serverById.get(t.getServerId());
            if (server == null) {
                throw new IllegalStateException("Server not found for tomcat target: " + t.getId());
            }
            TomcatTargetScanStateEntity state = tomcatStateById.get(t.getId());
            return new DecisionInputs.TomcatTargetObservation(
                    t.getId(),
                    server.getName(),
                    t.getRole(),
                    t.getBaseUrl(),
                    t.getPort(),
                    state == null ? null : state.getScannedAt(),
                    state == null ? null : state.getOutcomeKind(),
                    state == null ? null : state.getErrorKind(),
                    state == null ? null : state.getErrorMessage()
            );
        }).toList();

        List<DecisionInputs.ActuatorTargetObservation> actuatorObs = actuatorTargets.stream().map(t -> {
            ServerEntity server = serverById.get(t.getServerId());
            if (server == null) {
                throw new IllegalStateException("Server not found for actuator target: " + t.getId());
            }
            ActuatorTargetScanStateEntity state = actuatorStateById.get(t.getId());
            return new DecisionInputs.ActuatorTargetObservation(
                    t.getId(),
                    server.getName(),
                    t.getRole(),
                    t.getBaseUrl(),
                    t.getPort(),
                    t.getProfile(),
                    state == null ? null : state.getScannedAt(),
                    state == null ? null : state.getOutcomeKind(),
                    state == null ? null : state.getErrorKind(),
                    state == null ? null : state.getErrorMessage(),
                    state == null ? null : state.getHealthStatus(),
                    state == null ? null : state.getAppName(),
                    state == null ? null : state.getCpuUsage(),
                    state == null ? null : state.getMemoryUsedBytes()
            );
        }).toList();

        DecisionEvaluation evaluation = decisionEngine.evaluate(tomcatObs, actuatorObs);
        Instant now = Instant.now();

        return new EnvironmentStatusDto(
                env.getId(),
                env.getName(),
                evaluation.verdict(),
                now,
                evaluation.issues().stream().limit(MAX_ISSUES).toList()
        );
    }
}

