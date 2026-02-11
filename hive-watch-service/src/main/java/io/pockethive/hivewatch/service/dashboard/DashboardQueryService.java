package io.pockethive.hivewatch.service.dashboard;

import io.pockethive.hivewatch.service.api.DashboardEnvironmentDto;
import io.pockethive.hivewatch.service.api.TomcatEnvironmentStatus;
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

    public DashboardQueryService(
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
    public List<DashboardEnvironmentDto> listEnvironments() {
        List<EnvironmentEntity> environments = environmentRepository.findAll(Sort.by(Sort.Direction.ASC, "name"));
        List<ServerEntity> servers = serverRepository.findAll();
        List<TomcatTargetEntity> targets = tomcatTargetRepository.findAll();

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

        Map<UUID, TomcatTargetScanStateEntity> stateByTargetId = tomcatTargetScanStateRepository
                .findAllById(targets.stream().map(TomcatTargetEntity::getId).toList())
                .stream()
                .collect(java.util.stream.Collectors.toMap(TomcatTargetScanStateEntity::getTargetId, Function.identity()));

        List<DashboardEnvironmentDto> dtos = new ArrayList<>();
        for (EnvironmentEntity env : environments) {
            List<TomcatTargetEntity> envTargets = targetsByEnv.getOrDefault(env.getId(), List.of());

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

            TomcatEnvironmentStatus status = computeStatus(total, ok, err, envTargets, stateByTargetId);
            dtos.add(new DashboardEnvironmentDto(
                    env.getId(),
                    env.getName(),
                    total,
                    ok,
                    err,
                    webappsTotal,
                    lastScanAt,
                    status
            ));
        }

        dtos.sort(Comparator.comparing(DashboardEnvironmentDto::name));
        return dtos;
    }

    private static TomcatEnvironmentStatus computeStatus(
            int total,
            int ok,
            int err,
            List<TomcatTargetEntity> envTargets,
            Map<UUID, TomcatTargetScanStateEntity> stateByTargetId
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
}
