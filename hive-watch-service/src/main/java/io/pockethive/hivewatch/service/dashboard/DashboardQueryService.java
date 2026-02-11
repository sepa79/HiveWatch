package io.pockethive.hivewatch.service.dashboard;

import io.pockethive.hivewatch.service.api.DashboardCellDto;
import io.pockethive.hivewatch.service.api.DashboardCellKind;
import io.pockethive.hivewatch.service.api.DashboardColumnDto;
import io.pockethive.hivewatch.service.api.DashboardDto;
import io.pockethive.hivewatch.service.api.DashboardEnvironmentDto;
import io.pockethive.hivewatch.service.api.DashboardEnvironmentBlockDto;
import io.pockethive.hivewatch.service.api.DashboardEnvironmentSummaryDto;
import io.pockethive.hivewatch.service.api.DashboardGroupStatus;
import io.pockethive.hivewatch.service.api.DashboardGroupSummaryDto;
import io.pockethive.hivewatch.service.api.DashboardRowDto;
import io.pockethive.hivewatch.service.api.DashboardRowStatus;
import io.pockethive.hivewatch.service.api.DashboardSectionDto;
import io.pockethive.hivewatch.service.api.DashboardSectionKind;
import io.pockethive.hivewatch.service.api.ExpectedSetMode;
import io.pockethive.hivewatch.service.api.TomcatEnvironmentStatus;
import io.pockethive.hivewatch.service.api.TomcatRole;
import io.pockethive.hivewatch.service.api.TomcatWebappDto;
import io.pockethive.hivewatch.service.actuator.ActuatorTargetEntity;
import io.pockethive.hivewatch.service.actuator.ActuatorTargetRepository;
import io.pockethive.hivewatch.service.actuator.ActuatorTargetScanStateEntity;
import io.pockethive.hivewatch.service.actuator.ActuatorTargetScanStateRepository;
import io.pockethive.hivewatch.service.decision.DecisionEngine;
import io.pockethive.hivewatch.service.decision.DecisionEvaluation;
import io.pockethive.hivewatch.service.decision.DecisionInputs;
import io.pockethive.hivewatch.service.expectedsets.ExpectedSetTemplateItemEntity;
import io.pockethive.hivewatch.service.expectedsets.ExpectedSetTemplateItemRepository;
import io.pockethive.hivewatch.service.expectedsets.docker.DockerExpectedServiceEntity;
import io.pockethive.hivewatch.service.expectedsets.docker.DockerExpectedServiceRepository;
import io.pockethive.hivewatch.service.expectedsets.docker.DockerExpectedServiceSpecEntity;
import io.pockethive.hivewatch.service.expectedsets.docker.DockerExpectedServiceSpecRepository;
import io.pockethive.hivewatch.service.expectedsets.tomcat.TomcatExpectedWebappSpecEntity;
import io.pockethive.hivewatch.service.expectedsets.tomcat.TomcatExpectedWebappSpecRepository;
import io.pockethive.hivewatch.service.environments.EnvironmentEntity;
import io.pockethive.hivewatch.service.environments.servers.ServerEntity;
import io.pockethive.hivewatch.service.environments.servers.ServerRepository;
import io.pockethive.hivewatch.service.security.EnvironmentVisibilityService;
import io.pockethive.hivewatch.service.tomcat.TomcatTargetEntity;
import io.pockethive.hivewatch.service.tomcat.TomcatTargetRepository;
import io.pockethive.hivewatch.service.tomcat.TomcatTargetScanStateEntity;
import io.pockethive.hivewatch.service.tomcat.TomcatTargetScanStateRepository;
import io.pockethive.hivewatch.service.tomcat.expected.TomcatExpectedWebappEntity;
import io.pockethive.hivewatch.service.tomcat.expected.TomcatExpectedWebappRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardQueryService {
    private static final List<TomcatRoleColumn> TOMCAT_COLUMN_ORDER = List.of(
            new TomcatRoleColumn(TomcatRole.SERVICES, "SERVICES", "Services"),
            new TomcatRoleColumn(TomcatRole.AUTH, "AUTH", "Auth"),
            new TomcatRoleColumn(TomcatRole.PAYMENTS, "PAYMENTS", "Payments")
    );

    private static final Set<String> BUILT_IN_WEBAPPS = Set.of("/", "/manager", "/host-manager", "/docs", "/examples");

    private final ServerRepository serverRepository;
    private final TomcatTargetRepository tomcatTargetRepository;
    private final TomcatTargetScanStateRepository tomcatTargetScanStateRepository;
    private final TomcatExpectedWebappRepository tomcatExpectedWebappRepository;
    private final TomcatExpectedWebappSpecRepository tomcatExpectedWebappSpecRepository;
    private final ActuatorTargetRepository actuatorTargetRepository;
    private final ActuatorTargetScanStateRepository actuatorTargetScanStateRepository;
    private final DockerExpectedServiceSpecRepository dockerExpectedServiceSpecRepository;
    private final DockerExpectedServiceRepository dockerExpectedServiceRepository;
    private final ExpectedSetTemplateItemRepository expectedSetTemplateItemRepository;
    private final DecisionEngine decisionEngine;
    private final EnvironmentVisibilityService environmentVisibilityService;

    public DashboardQueryService(
            ServerRepository serverRepository,
            TomcatTargetRepository tomcatTargetRepository,
            TomcatTargetScanStateRepository tomcatTargetScanStateRepository,
            TomcatExpectedWebappRepository tomcatExpectedWebappRepository,
            TomcatExpectedWebappSpecRepository tomcatExpectedWebappSpecRepository,
            ActuatorTargetRepository actuatorTargetRepository,
            ActuatorTargetScanStateRepository actuatorTargetScanStateRepository,
            DockerExpectedServiceSpecRepository dockerExpectedServiceSpecRepository,
            DockerExpectedServiceRepository dockerExpectedServiceRepository,
            ExpectedSetTemplateItemRepository expectedSetTemplateItemRepository,
            DecisionEngine decisionEngine,
            EnvironmentVisibilityService environmentVisibilityService
    ) {
        this.serverRepository = serverRepository;
        this.tomcatTargetRepository = tomcatTargetRepository;
        this.tomcatTargetScanStateRepository = tomcatTargetScanStateRepository;
        this.tomcatExpectedWebappRepository = tomcatExpectedWebappRepository;
        this.tomcatExpectedWebappSpecRepository = tomcatExpectedWebappSpecRepository;
        this.actuatorTargetRepository = actuatorTargetRepository;
        this.actuatorTargetScanStateRepository = actuatorTargetScanStateRepository;
        this.dockerExpectedServiceSpecRepository = dockerExpectedServiceSpecRepository;
        this.dockerExpectedServiceRepository = dockerExpectedServiceRepository;
        this.expectedSetTemplateItemRepository = expectedSetTemplateItemRepository;
        this.decisionEngine = decisionEngine;
        this.environmentVisibilityService = environmentVisibilityService;
    }

    @Transactional(readOnly = true)
    public DashboardDto getDashboard() {
        Instant now = Instant.now();
        List<EnvironmentEntity> environments = environmentVisibilityService.listVisibleEnvironments();
        List<UUID> envIds = environments.stream().map(EnvironmentEntity::getId).toList();

        List<ServerEntity> servers = envIds.isEmpty() ? List.of() : serverRepository.findByEnvironmentIdIn(envIds);
        Map<UUID, ServerEntity> serverById = servers.stream()
                .collect(java.util.stream.Collectors.toMap(ServerEntity::getId, Function.identity()));

        List<UUID> serverIds = servers.stream().map(ServerEntity::getId).toList();
        List<TomcatTargetEntity> tomcatTargets = serverIds.isEmpty() ? List.of() : tomcatTargetRepository.findByServerIdIn(serverIds);
        List<ActuatorTargetEntity> actuatorTargets = serverIds.isEmpty() ? List.of() : actuatorTargetRepository.findByServerIdIn(serverIds);

        Map<UUID, UUID> envIdByServerId = servers.stream()
                .collect(java.util.stream.Collectors.toMap(ServerEntity::getId, ServerEntity::getEnvironmentId));

        Map<UUID, List<ServerEntity>> serversByEnv = new HashMap<>();
        for (ServerEntity s : servers) {
            serversByEnv.computeIfAbsent(s.getEnvironmentId(), ignored -> new ArrayList<>()).add(s);
        }

        Map<UUID, List<TomcatTargetEntity>> tomcatTargetsByEnv = new HashMap<>();
        for (TomcatTargetEntity t : tomcatTargets) {
            UUID envId = envIdByServerId.get(t.getServerId());
            if (envId == null) continue;
            tomcatTargetsByEnv.computeIfAbsent(envId, ignored -> new ArrayList<>()).add(t);
        }

        Map<UUID, List<ActuatorTargetEntity>> actuatorTargetsByEnv = new HashMap<>();
        for (ActuatorTargetEntity t : actuatorTargets) {
            UUID envId = envIdByServerId.get(t.getServerId());
            if (envId == null) continue;
            actuatorTargetsByEnv.computeIfAbsent(envId, ignored -> new ArrayList<>()).add(t);
        }

        Map<UUID, TomcatTargetScanStateEntity> tomcatStateByTargetId = tomcatTargetScanStateRepository
                .findAllById(tomcatTargets.stream().map(TomcatTargetEntity::getId).toList())
                .stream()
                .collect(java.util.stream.Collectors.toMap(TomcatTargetScanStateEntity::getTargetId, Function.identity()));

        Map<ExpectedKey, ExpectedSetSpec> tomcatExpectedSpecByServerRole = serverIds.isEmpty()
                ? Map.of()
                : tomcatExpectedWebappSpecRepository.findByServerIdIn(serverIds).stream()
                        .collect(java.util.stream.Collectors.toMap(
                                s -> new ExpectedKey(s.getServerId(), s.getRole()),
                                s -> new ExpectedSetSpec(s.getMode(), s.getTemplateId()),
                                (a, b) -> a
                        ));

        Map<ExpectedKey, Set<String>> explicitExpectedWebappsByServerRole = serverIds.isEmpty()
                ? Map.of()
                : tomcatExpectedWebappRepository
                        .findByServerIdIn(serverIds)
                        .stream()
                        .collect(java.util.stream.Collectors.groupingBy(
                                e -> new ExpectedKey(e.getServerId(), e.getRole()),
                                java.util.stream.Collectors.mapping(TomcatExpectedWebappEntity::getPath, java.util.stream.Collectors.toSet())
                        ));

        Map<UUID, ExpectedSetSpec> dockerExpectedSpecByServerId = serverIds.isEmpty()
                ? Map.of()
                : dockerExpectedServiceSpecRepository.findByServerIdIn(serverIds).stream()
                        .collect(java.util.stream.Collectors.toMap(
                                DockerExpectedServiceSpecEntity::getServerId,
                                s -> new ExpectedSetSpec(s.getMode(), s.getTemplateId()),
                                (a, b) -> a
                        ));

        Map<UUID, Set<String>> explicitExpectedDockerProfilesByServerId = serverIds.isEmpty()
                ? Map.of()
                : dockerExpectedServiceRepository.findByServerIdIn(serverIds).stream()
                        .collect(java.util.stream.Collectors.groupingBy(
                                DockerExpectedServiceEntity::getServerId,
                                java.util.stream.Collectors.mapping(DockerExpectedServiceEntity::getProfile, java.util.stream.Collectors.toSet())
                        ));

        Set<UUID> referencedTemplateIds = new HashSet<>();
        for (ExpectedSetSpec s : tomcatExpectedSpecByServerRole.values()) {
            if (s.mode() == ExpectedSetMode.TEMPLATE && s.templateId() != null) {
                referencedTemplateIds.add(s.templateId());
            }
        }
        for (ExpectedSetSpec s : dockerExpectedSpecByServerId.values()) {
            if (s.mode() == ExpectedSetMode.TEMPLATE && s.templateId() != null) {
                referencedTemplateIds.add(s.templateId());
            }
        }

        Map<UUID, Set<String>> templateItemsByTemplateId = referencedTemplateIds.isEmpty()
                ? Map.of()
                : expectedSetTemplateItemRepository.findByTemplateIdIn(List.copyOf(referencedTemplateIds)).stream()
                        .collect(java.util.stream.Collectors.groupingBy(
                                ExpectedSetTemplateItemEntity::getTemplateId,
                                java.util.stream.Collectors.mapping(ExpectedSetTemplateItemEntity::getValue, java.util.stream.Collectors.toSet())
                        ));

        Map<UUID, ActuatorTargetScanStateEntity> actuatorStateByTargetId = actuatorTargetScanStateRepository
                .findAllById(actuatorTargets.stream().map(ActuatorTargetEntity::getId).toList())
                .stream()
                .collect(java.util.stream.Collectors.toMap(ActuatorTargetScanStateEntity::getTargetId, Function.identity()));

        List<DashboardEnvironmentBlockDto> blocks = new ArrayList<>();
        for (EnvironmentEntity env : environments) {
            List<TomcatTargetEntity> envTomcats = tomcatTargetsByEnv.getOrDefault(env.getId(), List.of());
            List<ActuatorTargetEntity> envActuators = actuatorTargetsByEnv.getOrDefault(env.getId(), List.of());

            DashboardGroupSummaryDto tomcatsSummary = computeTomcatsGroupSummary(envTomcats, tomcatStateByTargetId);
            DashboardGroupSummaryDto dockerSummary = computeDockerGroupSummary(envActuators, actuatorStateByTargetId);
            DashboardGroupSummaryDto awsSummary = new DashboardGroupSummaryDto(DashboardGroupStatus.UNKNOWN, 0, null);

            List<DecisionInputs.TomcatTargetObservation> tomcatObs = envTomcats.stream().map(t -> {
                ServerEntity s = serverById.get(t.getServerId());
                if (s == null) {
                    throw new IllegalStateException("Server not found for tomcat target: " + t.getId());
                }
                TomcatTargetScanStateEntity st = tomcatStateByTargetId.get(t.getId());
                return new DecisionInputs.TomcatTargetObservation(
                        t.getId(),
                        s.getName(),
                        t.getRole(),
                        t.getBaseUrl(),
                        t.getPort(),
                        st == null ? null : st.getScannedAt(),
                        st == null ? null : st.getOutcomeKind(),
                        st == null ? null : st.getErrorKind(),
                        st == null ? null : st.getErrorMessage()
                );
            }).toList();

            List<DecisionInputs.ActuatorTargetObservation> actuatorObs = envActuators.stream().map(t -> {
                ServerEntity s = serverById.get(t.getServerId());
                if (s == null) {
                    throw new IllegalStateException("Server not found for actuator target: " + t.getId());
                }
                ActuatorTargetScanStateEntity st = actuatorStateByTargetId.get(t.getId());
                return new DecisionInputs.ActuatorTargetObservation(
                        t.getId(),
                        s.getName(),
                        t.getRole(),
                        t.getBaseUrl(),
                        t.getPort(),
                        t.getProfile(),
                        st == null ? null : st.getScannedAt(),
                        st == null ? null : st.getOutcomeKind(),
                        st == null ? null : st.getErrorKind(),
                        st == null ? null : st.getErrorMessage(),
                        st == null ? null : st.getHealthStatus(),
                        st == null ? null : st.getAppName(),
                        st == null ? null : st.getCpuUsage(),
                        st == null ? null : st.getMemoryUsedBytes()
                );
            }).toList();

            DecisionEvaluation decision = decisionEngine.evaluate(tomcatObs, actuatorObs);
            DashboardEnvironmentSummaryDto summary = new DashboardEnvironmentSummaryDto(
                    tomcatsSummary,
                    dockerSummary,
                    awsSummary,
                    decision.verdict(),
                    decision.blockIssues(),
                    decision.warnIssues(),
                    decision.unknownIssues(),
                    now
            );

            List<DashboardSectionDto> sections = new ArrayList<>();
            sections.add(computeTomcatsSection(
                    env.getId(),
                    serversByEnv.getOrDefault(env.getId(), List.of()),
                    envTomcats,
                    tomcatStateByTargetId,
                    tomcatExpectedSpecByServerRole,
                    explicitExpectedWebappsByServerRole,
                    templateItemsByTemplateId
            ));
            sections.add(computeDockerSection(
                    env.getId(),
                    serversByEnv.getOrDefault(env.getId(), List.of()),
                    envActuators,
                    actuatorStateByTargetId,
                    dockerExpectedSpecByServerId,
                    explicitExpectedDockerProfilesByServerId,
                    templateItemsByTemplateId
            ));
            sections.add(new DashboardSectionDto(DashboardSectionKind.AWS, "AWS (placeholder)", List.of(), List.of()));

            blocks.add(new DashboardEnvironmentBlockDto(env.getId(), env.getName(), summary, sections));
        }

        blocks.sort(Comparator.comparing(DashboardEnvironmentBlockDto::name));
        return new DashboardDto(List.copyOf(blocks));
    }

    @Transactional(readOnly = true)
    public List<DashboardEnvironmentDto> listEnvironments() {
        List<EnvironmentEntity> environments = environmentVisibilityService.listVisibleEnvironments();
        List<UUID> envIds = environments.stream().map(EnvironmentEntity::getId).toList();

        List<ServerEntity> servers = envIds.isEmpty() ? List.of() : serverRepository.findByEnvironmentIdIn(envIds);
        Map<UUID, ServerEntity> serverById = servers.stream()
                .collect(java.util.stream.Collectors.toMap(ServerEntity::getId, Function.identity()));

        List<UUID> serverIds = servers.stream().map(ServerEntity::getId).toList();
        List<TomcatTargetEntity> targets = serverIds.isEmpty() ? List.of() : tomcatTargetRepository.findByServerIdIn(serverIds);
        List<ActuatorTargetEntity> actuatorTargets = serverIds.isEmpty() ? List.of() : actuatorTargetRepository.findByServerIdIn(serverIds);

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

            List<DecisionInputs.TomcatTargetObservation> tomcatObs = envTargets.stream().map(t -> {
                ServerEntity s = serverById.get(t.getServerId());
                if (s == null) {
                    throw new IllegalStateException("Server not found for tomcat target: " + t.getId());
                }
                TomcatTargetScanStateEntity st = stateByTargetId.get(t.getId());
                return new DecisionInputs.TomcatTargetObservation(
                        t.getId(),
                        s.getName(),
                        t.getRole(),
                        t.getBaseUrl(),
                        t.getPort(),
                        st == null ? null : st.getScannedAt(),
                        st == null ? null : st.getOutcomeKind(),
                        st == null ? null : st.getErrorKind(),
                        st == null ? null : st.getErrorMessage()
                );
            }).toList();

            List<DecisionInputs.ActuatorTargetObservation> actuatorObs = envActuatorTargets.stream().map(t -> {
                ServerEntity s = serverById.get(t.getServerId());
                if (s == null) {
                    throw new IllegalStateException("Server not found for actuator target: " + t.getId());
                }
                ActuatorTargetScanStateEntity st = actuatorStateByTargetId.get(t.getId());
                return new DecisionInputs.ActuatorTargetObservation(
                        t.getId(),
                        s.getName(),
                        t.getRole(),
                        t.getBaseUrl(),
                        t.getPort(),
                        t.getProfile(),
                        st == null ? null : st.getScannedAt(),
                        st == null ? null : st.getOutcomeKind(),
                        st == null ? null : st.getErrorKind(),
                        st == null ? null : st.getErrorMessage(),
                        st == null ? null : st.getHealthStatus(),
                        st == null ? null : st.getAppName(),
                        st == null ? null : st.getCpuUsage(),
                        st == null ? null : st.getMemoryUsedBytes()
                );
            }).toList();

            DecisionEvaluation decision = decisionEngine.evaluate(tomcatObs, actuatorObs);

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
                    actuatorStatus,
                    decision.verdict(),
                    decision.blockIssues(),
                    decision.warnIssues(),
                    decision.unknownIssues()
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

    private static DashboardGroupSummaryDto computeTomcatsGroupSummary(
            List<TomcatTargetEntity> targets,
            Map<UUID, TomcatTargetScanStateEntity> stateByTargetId
    ) {
        if (targets.isEmpty()) {
            return new DashboardGroupSummaryDto(DashboardGroupStatus.UNKNOWN, 0, null);
        }

        boolean hasError = false;
        boolean hasUnknown = false;
        Instant lastScanAt = null;

        for (TomcatTargetEntity t : targets) {
            TomcatTargetScanStateEntity st = stateByTargetId.get(t.getId());
            if (st == null) {
                hasUnknown = true;
                continue;
            }
            if (lastScanAt == null || st.getScannedAt().isAfter(lastScanAt)) {
                lastScanAt = st.getScannedAt();
            }
            if (st.getOutcomeKind() != io.pockethive.hivewatch.service.api.TomcatScanOutcomeKind.SUCCESS) {
                hasError = true;
            }
        }

        DashboardGroupStatus status = hasError ? DashboardGroupStatus.BLOCK : (hasUnknown ? DashboardGroupStatus.UNKNOWN : DashboardGroupStatus.OK);
        return new DashboardGroupSummaryDto(status, targets.size(), lastScanAt);
    }

    private static DashboardGroupSummaryDto computeDockerGroupSummary(
            List<ActuatorTargetEntity> targets,
            Map<UUID, ActuatorTargetScanStateEntity> stateByTargetId
    ) {
        if (targets.isEmpty()) {
            return new DashboardGroupSummaryDto(DashboardGroupStatus.UNKNOWN, 0, null);
        }

        boolean hasError = false;
        boolean hasUnknown = false;
        Instant lastScanAt = null;

        for (ActuatorTargetEntity t : targets) {
            ActuatorTargetScanStateEntity st = stateByTargetId.get(t.getId());
            if (st == null) {
                hasUnknown = true;
                continue;
            }
            if (lastScanAt == null || st.getScannedAt().isAfter(lastScanAt)) {
                lastScanAt = st.getScannedAt();
            }
            if (st.getOutcomeKind() != io.pockethive.hivewatch.service.api.TomcatScanOutcomeKind.SUCCESS) {
                hasError = true;
                continue;
            }
            String hs = st.getHealthStatus();
            if (!"UP".equalsIgnoreCase((hs == null ? "" : hs).trim())) {
                hasError = true;
            }
        }

        DashboardGroupStatus status = hasError ? DashboardGroupStatus.BLOCK : (hasUnknown ? DashboardGroupStatus.UNKNOWN : DashboardGroupStatus.OK);
        return new DashboardGroupSummaryDto(status, targets.size(), lastScanAt);
    }

    private DashboardSectionDto computeTomcatsSection(
            UUID environmentId,
            List<ServerEntity> envServers,
            List<TomcatTargetEntity> envTargets,
            Map<UUID, TomcatTargetScanStateEntity> stateByTargetId,
            Map<ExpectedKey, ExpectedSetSpec> expectedSpecByServerRole,
            Map<ExpectedKey, Set<String>> explicitExpectedWebappsByServerRole,
            Map<UUID, Set<String>> templateItemsByTemplateId
    ) {
        Map<UUID, ServerEntity> serverById = envServers.stream()
                .collect(java.util.stream.Collectors.toMap(ServerEntity::getId, Function.identity()));

        Map<UUID, List<TomcatTargetEntity>> targetsByServerId = new HashMap<>();
        for (TomcatTargetEntity t : envTargets) {
            targetsByServerId.computeIfAbsent(t.getServerId(), ignored -> new ArrayList<>()).add(t);
        }

        List<DashboardColumnDto> columns = new ArrayList<>();
        for (TomcatRoleColumn c : TOMCAT_COLUMN_ORDER) {
            columns.add(new DashboardColumnDto(c.key(), c.label()));
        }
        columns.add(new DashboardColumnDto("TOMCAT", "Tomcat"));
        columns.add(new DashboardColumnDto("JAVA", "Java"));
        columns.add(new DashboardColumnDto("OS", "OS"));

        List<DashboardRowDto> rows = new ArrayList<>();
        for (Map.Entry<UUID, List<TomcatTargetEntity>> e : targetsByServerId.entrySet()) {
            UUID serverId = e.getKey();
            List<TomcatTargetEntity> targets = e.getValue();
            ServerEntity server = serverById.get(serverId);
            String serverName = server == null ? "Unknown" : server.getName();

            Map<TomcatRole, TomcatTargetEntity> byRole = new HashMap<>();
            for (TomcatTargetEntity t : targets) {
                byRole.put(t.getRole(), t);
            }

            List<DashboardCellDto> cells = new ArrayList<>();
            for (TomcatRoleColumn c : TOMCAT_COLUMN_ORDER) {
                TomcatTargetEntity t = byRole.get(c.role());
                if (t == null) {
                    cells.add(new DashboardCellDto(DashboardCellKind.ERROR, null, "Missing target: " + c.role().name()));
                    continue;
                }
                TomcatTargetScanStateEntity st = stateByTargetId.get(t.getId());
                ExpectedKey expectedKey = new ExpectedKey(serverId, c.role());
                ExpectedSetSpec spec = expectedSpecByServerRole.get(expectedKey);
                Set<String> expected = expectedSetForSpec(spec, expectedKey, explicitExpectedWebappsByServerRole, templateItemsByTemplateId);
                DashboardCellDto cell = tomcatRoleCell(c.role(), st, expected);
                cells.add(cell);
            }

            DashboardCellDto tomcatVersion = uniformStringCell(
                    TOMCAT_COLUMN_ORDER.stream()
                            .map(c -> byRole.get(c.role()))
                            .map(t -> t == null ? null : stateByTargetId.get(t.getId()))
                            .map(st -> st == null ? null : st.getTomcatVersion())
                            .toList()
            );
            DashboardCellDto javaVersion = uniformStringCell(
                    TOMCAT_COLUMN_ORDER.stream()
                            .map(c -> byRole.get(c.role()))
                            .map(t -> t == null ? null : stateByTargetId.get(t.getId()))
                            .map(st -> st == null ? null : st.getJavaVersion())
                            .toList()
            );
            DashboardCellDto os = uniformStringCell(
                    TOMCAT_COLUMN_ORDER.stream()
                            .map(c -> byRole.get(c.role()))
                            .map(t -> t == null ? null : stateByTargetId.get(t.getId()))
                            .map(st -> st == null ? null : st.getOs())
                            .toList()
            );
            cells.add(tomcatVersion);
            cells.add(javaVersion);
            cells.add(os);

            DashboardRowStatus status = computeRowStatus(cells);
            rows.add(new DashboardRowDto(serverId, serverName, null, List.copyOf(cells), status));
        }

        rows.sort(Comparator.comparing(DashboardRowDto::label));
        return new DashboardSectionDto(DashboardSectionKind.TOMCATS, "Tomcats", List.copyOf(columns), List.copyOf(rows));
    }

    private DashboardSectionDto computeDockerSection(
            UUID environmentId,
            List<ServerEntity> envServers,
            List<ActuatorTargetEntity> envTargets,
            Map<UUID, ActuatorTargetScanStateEntity> stateByTargetId,
            Map<UUID, ExpectedSetSpec> expectedSpecByServerId,
            Map<UUID, Set<String>> explicitExpectedProfilesByServerId,
            Map<UUID, Set<String>> templateItemsByTemplateId
    ) {
        if (envTargets.isEmpty()) {
            return new DashboardSectionDto(DashboardSectionKind.DOCKER, "Docker Swarm", List.of(), List.of());
        }

        Map<UUID, ServerEntity> serverById = envServers.stream()
                .collect(java.util.stream.Collectors.toMap(ServerEntity::getId, Function.identity()));

        Map<UUID, List<ActuatorTargetEntity>> targetsByServerId = new HashMap<>();
        Set<String> profiles = new HashSet<>();
        for (ActuatorTargetEntity t : envTargets) {
            targetsByServerId.computeIfAbsent(t.getServerId(), ignored -> new ArrayList<>()).add(t);
            profiles.add(t.getProfile());
        }

        Map<UUID, Set<String>> expectedProfilesByServerId = new HashMap<>();
        for (UUID serverId : targetsByServerId.keySet()) {
            ExpectedSetSpec spec = expectedSpecByServerId.get(serverId);
            Set<String> expected = expectedSetForSpec(spec, serverId, explicitExpectedProfilesByServerId, templateItemsByTemplateId);
            expectedProfilesByServerId.put(serverId, expected);
            profiles.addAll(expected);
        }

        List<String> profileOrder = new ArrayList<>(profiles);
        List<String> defaultOrder = List.of("payments", "services", "auth");
        profileOrder.sort((a, b) -> {
            int ai = defaultOrder.indexOf(a);
            int bi = defaultOrder.indexOf(b);
            if (ai != -1 || bi != -1) return (ai == -1 ? 999 : ai) - (bi == -1 ? 999 : bi);
            return a.compareToIgnoreCase(b);
        });

        List<DashboardColumnDto> columns = profileOrder.stream()
                .map(p -> new DashboardColumnDto(p, p))
                .toList();

        List<DashboardRowDto> rows = new ArrayList<>();
        for (Map.Entry<UUID, List<ActuatorTargetEntity>> e : targetsByServerId.entrySet()) {
            UUID serverId = e.getKey();
            List<ActuatorTargetEntity> targets = e.getValue();
            ServerEntity server = serverById.get(serverId);
            String serverName = server == null ? "Unknown" : server.getName();

            Map<String, ActuatorTargetEntity> byProfile = new HashMap<>();
            for (ActuatorTargetEntity t : targets) {
                byProfile.put(t.getProfile(), t);
            }

            List<DashboardCellDto> cells = new ArrayList<>();
            Set<String> expectedProfiles = expectedProfilesByServerId.getOrDefault(serverId, Set.of());
            for (String profile : profileOrder) {
                ActuatorTargetEntity t = byProfile.get(profile);
                if (t == null) {
                    if (expectedProfiles.contains(profile)) {
                        cells.add(new DashboardCellDto(DashboardCellKind.ERROR, null, "Missing expected service: " + profile));
                    } else {
                        cells.add(new DashboardCellDto(DashboardCellKind.UNKNOWN, null, null));
                    }
                    continue;
                }
                ActuatorTargetScanStateEntity st = stateByTargetId.get(t.getId());
                cells.add(dockerServiceCell(profile, st));
            }

            DashboardRowStatus status = computeRowStatus(cells);
            String link = "/dashboard/docker/" + environmentId + "/" + serverId;
            rows.add(new DashboardRowDto(serverId, serverName, link, List.copyOf(cells), status));
        }

        rows.sort(Comparator.comparing(DashboardRowDto::label));
        return new DashboardSectionDto(DashboardSectionKind.DOCKER, "Docker Swarm", columns, List.copyOf(rows));
    }

    private static Set<String> expectedSetForSpec(
            ExpectedSetSpec spec,
            ExpectedKey key,
            Map<ExpectedKey, Set<String>> explicit,
            Map<UUID, Set<String>> templateItemsByTemplateId
    ) {
        if (spec == null || spec.mode() == ExpectedSetMode.UNCONFIGURED) {
            return Set.of();
        }
        if (spec.mode() == ExpectedSetMode.EXPLICIT) {
            return explicit.getOrDefault(key, Set.of());
        }
        if (spec.mode() == ExpectedSetMode.TEMPLATE && spec.templateId() != null) {
            return templateItemsByTemplateId.getOrDefault(spec.templateId(), Set.of());
        }
        return Set.of();
    }

    private static Set<String> expectedSetForSpec(
            ExpectedSetSpec spec,
            UUID serverId,
            Map<UUID, Set<String>> explicit,
            Map<UUID, Set<String>> templateItemsByTemplateId
    ) {
        if (spec == null || spec.mode() == ExpectedSetMode.UNCONFIGURED) {
            return Set.of();
        }
        if (spec.mode() == ExpectedSetMode.EXPLICIT) {
            return explicit.getOrDefault(serverId, Set.of());
        }
        if (spec.mode() == ExpectedSetMode.TEMPLATE && spec.templateId() != null) {
            return templateItemsByTemplateId.getOrDefault(spec.templateId(), Set.of());
        }
        return Set.of();
    }

    private static DashboardRowStatus computeRowStatus(List<DashboardCellDto> cells) {
        if (cells.stream().anyMatch(c -> c.kind() == DashboardCellKind.ERROR)) return DashboardRowStatus.BLOCK;
        if (cells.stream().anyMatch(c -> c.kind() == DashboardCellKind.UNKNOWN)) return DashboardRowStatus.UNKNOWN;
        return DashboardRowStatus.OK;
    }

    private static DashboardCellDto tomcatRoleCell(TomcatRole role, TomcatTargetScanStateEntity st, Set<String> expectedPaths) {
        if (st == null) {
            if (expectedPaths != null && !expectedPaths.isEmpty()) {
                String title = "Missing expected webapps: " + String.join(", ", expectedPaths.stream().limit(6).toList());
                return new DashboardCellDto(DashboardCellKind.ERROR, null, role.name() + " mismatch: " + title);
            }
            return new DashboardCellDto(DashboardCellKind.UNKNOWN, null, null);
        }
        if (st.getOutcomeKind() != io.pockethive.hivewatch.service.api.TomcatScanOutcomeKind.SUCCESS) {
            String title = role.name() + " error: " + (st.getErrorKind() == null ? "UNKNOWN" : st.getErrorKind().name())
                    + ": " + (st.getErrorMessage() == null ? "Request failed" : st.getErrorMessage());
            return new DashboardCellDto(DashboardCellKind.ERROR, null, title);
        }

        List<TomcatWebappDto> relevant = st.getWebapps().stream()
                .filter(w -> !BUILT_IN_WEBAPPS.contains(w.path()))
                .toList();

        Set<String> presentPaths = relevant.stream()
                .map(w -> w.path() == null ? "" : w.path().trim())
                .filter(p -> !p.isEmpty())
                .collect(java.util.stream.Collectors.toSet());

        if (expectedPaths != null && !expectedPaths.isEmpty()) {
            List<String> missingExpected = expectedPaths.stream()
                    .filter(p -> !presentPaths.contains(p))
                    .sorted(String::compareToIgnoreCase)
                    .toList();
            if (!missingExpected.isEmpty()) {
                String title = "Missing expected webapps: " + String.join(", ", missingExpected.stream().limit(6).toList());
                return new DashboardCellDto(DashboardCellKind.ERROR, null, role.name() + " mismatch: " + title);
            }
        }

        if (relevant.isEmpty()) {
            return new DashboardCellDto(DashboardCellKind.UNKNOWN, null, null);
        }

        Set<String> versions = new HashSet<>();
        List<String> missing = new ArrayList<>();
        for (TomcatWebappDto w : relevant) {
            String v = w.version();
            if (v == null || v.trim().isEmpty()) {
                missing.add(w.name());
                continue;
            }
            versions.add(v.trim());
        }

        if (!missing.isEmpty()) {
            String title = "Missing webapp versions: " + String.join(", ", missing.stream().limit(6).toList());
            return new DashboardCellDto(DashboardCellKind.ERROR, null, role.name() + " mismatch: " + title);
        }
        if (versions.size() == 1) {
            return new DashboardCellDto(DashboardCellKind.VALUE, versions.iterator().next(), null);
        }
        String title = "Multiple webapp versions: " + String.join(", ", versions);
        return new DashboardCellDto(DashboardCellKind.ERROR, null, role.name() + " mismatch: " + title);
    }

    private static DashboardCellDto dockerServiceCell(String profile, ActuatorTargetScanStateEntity st) {
        if (st == null) {
            return new DashboardCellDto(DashboardCellKind.UNKNOWN, null, null);
        }
        if (st.getOutcomeKind() != io.pockethive.hivewatch.service.api.TomcatScanOutcomeKind.SUCCESS) {
            String title = profile + " error: " + (st.getErrorKind() == null ? "UNKNOWN" : st.getErrorKind().name())
                    + ": " + (st.getErrorMessage() == null ? "Request failed" : st.getErrorMessage());
            return new DashboardCellDto(DashboardCellKind.ERROR, null, title);
        }
        if (!"UP".equalsIgnoreCase((st.getHealthStatus() == null ? "" : st.getHealthStatus()).trim())) {
            return new DashboardCellDto(DashboardCellKind.ERROR, null, profile + " is " + (st.getHealthStatus() == null ? "UNKNOWN" : st.getHealthStatus()));
        }
        String version = st.getBuildVersion();
        if (version == null || version.trim().isEmpty()) {
            return new DashboardCellDto(DashboardCellKind.UNKNOWN, null, st.getAppName());
        }
        return new DashboardCellDto(DashboardCellKind.VALUE, version.trim(), st.getAppName());
    }

    private static DashboardCellDto uniformStringCell(List<String> values) {
        List<String> present = values.stream()
                .map(v -> v == null ? "" : v.trim())
                .filter(v -> !v.isEmpty())
                .toList();
        if (present.isEmpty()) return new DashboardCellDto(DashboardCellKind.UNKNOWN, null, null);
        Set<String> uniq = new HashSet<>(present);
        if (uniq.size() == 1) return new DashboardCellDto(DashboardCellKind.VALUE, present.get(0), null);
        return new DashboardCellDto(DashboardCellKind.ERROR, null, "Multiple values: " + String.join(" · ", uniq));
    }

    private record TomcatRoleColumn(TomcatRole role, String key, String label) {
    }

    private record ExpectedKey(UUID serverId, TomcatRole role) {
    }

    private record ExpectedSetSpec(ExpectedSetMode mode, UUID templateId) {
    }
}
