package io.pockethive.hivewatch.service.dashboard;

import io.pockethive.hivewatch.service.api.DashboardEnvironmentDto;
import io.pockethive.hivewatch.service.api.TomcatEnvironmentStatus;
import io.pockethive.hivewatch.service.actuator.ActuatorTargetEntity;
import io.pockethive.hivewatch.service.actuator.ActuatorTargetRepository;
import io.pockethive.hivewatch.service.actuator.ActuatorTargetScanStateEntity;
import io.pockethive.hivewatch.service.actuator.ActuatorTargetScanStateRepository;
import io.pockethive.hivewatch.service.environments.EnvironmentEntity;
import io.pockethive.hivewatch.service.environments.EnvironmentRepository;
import io.pockethive.hivewatch.service.environments.servers.ServerEntity;
import io.pockethive.hivewatch.service.environments.servers.ServerRepository;
import io.pockethive.hivewatch.service.tomcat.TomcatTargetEntity;
import io.pockethive.hivewatch.service.tomcat.TomcatTargetRepository;
import io.pockethive.hivewatch.service.tomcat.TomcatTargetScanStateEntity;
import io.pockethive.hivewatch.service.tomcat.TomcatTargetScanStateRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardQueryService {
    private final EnvironmentRepository environmentRepository;
    private final ServerRepository serverRepository;
    private final TomcatTargetRepository tomcatTargetRepository;
    private final TomcatTargetScanStateRepository tomcatTargetScanStateRepository;
    private final ActuatorTargetRepository actuatorTargetRepository;
    private final ActuatorTargetScanStateRepository actuatorTargetScanStateRepository;

    public DashboardQueryService(
            EnvironmentRepository environmentRepository,
            ServerRepository serverRepository,
            TomcatTargetRepository tomcatTargetRepository,
            TomcatTargetScanStateRepository tomcatTargetScanStateRepository,
            ActuatorTargetRepository actuatorTargetRepository,
            ActuatorTargetScanStateRepository actuatorTargetScanStateRepository
    ) {
        this.environmentRepository = environmentRepository;
        this.serverRepository = serverRepository;
        this.tomcatTargetRepository = tomcatTargetRepository;
        this.tomcatTargetScanStateRepository = tomcatTargetScanStateRepository;
        this.actuatorTargetRepository = actuatorTargetRepository;
        this.actuatorTargetScanStateRepository = actuatorTargetScanStateRepository;
    }

    @Transactional(readOnly = true)
    public List<DashboardEnvironmentDto> listEnvironments() {
        List<EnvironmentEntity> environments = environmentRepository.findAll(Sort.by(Sort.Direction.ASC, "name"));
        List<ServerEntity> servers = serverRepository.findAll();
        List<TomcatTargetEntity> targets = tomcatTargetRepository.findAll();
        List<ActuatorTargetEntity> actuatorTargets = actuatorTargetRepository.findAll();

        Map<UUID, UUID> serverEnvByServerId = servers.stream()
                .collect(java.util.stream.Collectors.toMap(ServerEntity::getId, ServerEntity::getEnvironmentId));

        Map<UUID, List<TomcatTargetEntity>> targetsByEnv = new HashMap<>();
        for (TomcatTargetEntity t : targets) {
            UUID envId = serverEnvByServerId.get(t.getServerId());
            if (envId == null) {
                continue;
            }
            targetsByEnv.computeIfAbsent(envId, ignored -> new ArrayList<>()).add(t);
        }

        Map<UUID, List<ActuatorTargetEntity>> actuatorTargetsByEnv = new HashMap<>();
        for (ActuatorTargetEntity t : actuatorTargets) {
            UUID envId = serverEnvByServerId.get(t.getServerId());
            if (envId == null) {
                continue;
            }
            actuatorTargetsByEnv.computeIfAbsent(envId, ignored -> new ArrayList<>()).add(t);
        }

        Map<UUID, TomcatTargetScanStateEntity> stateByTargetId = tomcatTargetScanStateRepository
                .findAllById(targets.stream().map(TomcatTargetEntity::getId).toList())
                .stream()
                .collect(java.util.stream.Collectors.toMap(TomcatTargetScanStateEntity::getTargetId, Function.identity()));

        Map<UUID, ActuatorTargetScanStateEntity> actuatorStateByTargetId = actuatorTargetScanStateRepository
                .findAllById(actuatorTargets.stream().map(ActuatorTargetEntity::getId).toList())
                .stream()
                .collect(java.util.stream.Collectors.toMap(ActuatorTargetScanStateEntity::getTargetId, Function.identity()));

        List<DashboardEnvironmentDto> dtos = new ArrayList<>();
        for (EnvironmentEntity env : environments) {
            List<TomcatTargetEntity> envTargets = targetsByEnv.getOrDefault(env.getId(), List.of());
            List<ActuatorTargetEntity> envActuatorTargets = actuatorTargetsByEnv.getOrDefault(env.getId(), List.of());

            int total = envTargets.size();
            int ok = 0;
            int err = 0;
            int webappsTotal = 0;
            Instant lastScanAt = null;

            for (TomcatTargetEntity t : envTargets) {
                TomcatTargetScanStateEntity state = stateByTargetId.get(t.getId());
                if (state == null) {
                    continue;
                }
                if (lastScanAt == null || state.getScannedAt().isAfter(lastScanAt)) {
                    lastScanAt = state.getScannedAt();
                }
                switch (state.getOutcomeKind()) {
                    case SUCCESS -> {
                        ok++;
                        webappsTotal += state.getWebapps().size();
                    }
                    case ERROR -> err++;
                }
            }

            TomcatEnvironmentStatus status = computeStatus(total, ok, err);

            int aTotal = envActuatorTargets.size();
            int aUp = 0;
            int aDown = 0;
            int aErr = 0;
            Instant aLastScanAt = null;
            for (ActuatorTargetEntity t : envActuatorTargets) {
                ActuatorTargetScanStateEntity state = actuatorStateByTargetId.get(t.getId());
                if (state == null) {
                    continue;
                }
                if (aLastScanAt == null || state.getScannedAt().isAfter(aLastScanAt)) {
                    aLastScanAt = state.getScannedAt();
                }
                switch (state.getOutcomeKind()) {
                    case SUCCESS -> {
                        String hs = state.getHealthStatus();
                        if ("UP".equalsIgnoreCase(hs)) {
                            aUp++;
                        } else if ("DOWN".equalsIgnoreCase(hs)) {
                            aDown++;
                        } else {
                            aDown++;
                        }
                    }
                    case ERROR -> aErr++;
                }
            }
            TomcatEnvironmentStatus actuatorStatus = computeActuatorStatus(aTotal, aUp, aDown, aErr);

            dtos.add(new DashboardEnvironmentDto(
                    env.getId(),
                    env.getName(),
                    total,
                    ok,
                    err,
                    webappsTotal,
                    lastScanAt,
                    status,
                    aTotal,
                    aUp,
                    aDown,
                    aErr,
                    aLastScanAt,
                    actuatorStatus
            ));
        }

        dtos.sort(Comparator.comparing(DashboardEnvironmentDto::name));
        return dtos;
    }

    private static TomcatEnvironmentStatus computeStatus(
            int total,
            int ok,
            int err
    ) {
        if (total == 0) {
            return TomcatEnvironmentStatus.UNKNOWN;
        }
        if (err > 0) {
            return TomcatEnvironmentStatus.BLOCK;
        }
        if (ok == total) {
            return TomcatEnvironmentStatus.OK;
        }
        return TomcatEnvironmentStatus.UNKNOWN;
    }

    private static TomcatEnvironmentStatus computeActuatorStatus(int total, int up, int down, int err) {
        if (total == 0) {
            return TomcatEnvironmentStatus.UNKNOWN;
        }
        if (err > 0) {
            return TomcatEnvironmentStatus.BLOCK;
        }
        if (down > 0) {
            return TomcatEnvironmentStatus.BLOCK;
        }
        if (up == total) {
            return TomcatEnvironmentStatus.OK;
        }
        return TomcatEnvironmentStatus.UNKNOWN;
    }
}
