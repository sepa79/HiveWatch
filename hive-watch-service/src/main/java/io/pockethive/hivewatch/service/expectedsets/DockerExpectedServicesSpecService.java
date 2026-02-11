package io.pockethive.hivewatch.service.expectedsets;

import io.pockethive.hivewatch.service.api.DockerExpectedServicesSpecDto;
import io.pockethive.hivewatch.service.api.DockerExpectedServicesSpecReplaceRequestDto;
import io.pockethive.hivewatch.service.api.ExpectedSetMode;
import io.pockethive.hivewatch.service.api.ExpectedSetTemplateKind;
import io.pockethive.hivewatch.service.environments.EnvironmentRepository;
import io.pockethive.hivewatch.service.environments.servers.ServerEntity;
import io.pockethive.hivewatch.service.environments.servers.ServerRepository;
import io.pockethive.hivewatch.service.expectedsets.docker.DockerExpectedServiceEntity;
import io.pockethive.hivewatch.service.expectedsets.docker.DockerExpectedServiceRepository;
import io.pockethive.hivewatch.service.expectedsets.docker.DockerExpectedServiceSpecEntity;
import io.pockethive.hivewatch.service.expectedsets.docker.DockerExpectedServiceSpecRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
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
public class DockerExpectedServicesSpecService {
    private final EnvironmentRepository environmentRepository;
    private final ServerRepository serverRepository;
    private final DockerExpectedServiceRepository explicitRepository;
    private final DockerExpectedServiceSpecRepository specRepository;
    private final ExpectedSetTemplateRepository templateRepository;
    private final ExpectedSetTemplateItemRepository templateItemRepository;

    public DockerExpectedServicesSpecService(
            EnvironmentRepository environmentRepository,
            ServerRepository serverRepository,
            DockerExpectedServiceRepository explicitRepository,
            DockerExpectedServiceSpecRepository specRepository,
            ExpectedSetTemplateRepository templateRepository,
            ExpectedSetTemplateItemRepository templateItemRepository
    ) {
        this.environmentRepository = environmentRepository;
        this.serverRepository = serverRepository;
        this.explicitRepository = explicitRepository;
        this.specRepository = specRepository;
        this.templateRepository = templateRepository;
        this.templateItemRepository = templateItemRepository;
    }

    @Transactional(readOnly = true)
    public List<DockerExpectedServicesSpecDto> list(UUID environmentId) {
        requireEnvironment(environmentId);

        List<ServerEntity> servers = serverRepository.findByEnvironmentId(environmentId);
        if (servers.isEmpty()) return List.of();

        Map<UUID, ServerEntity> serverById = servers.stream()
                .collect(java.util.stream.Collectors.toMap(ServerEntity::getId, Function.identity()));

        List<UUID> serverIds = servers.stream().map(ServerEntity::getId).toList();

        Map<UUID, DockerExpectedServiceSpecEntity> specByServerId = specRepository.findByServerIdIn(serverIds).stream()
                .collect(java.util.stream.Collectors.toMap(DockerExpectedServiceSpecEntity::getServerId, Function.identity(), (a, b) -> a));

        Map<UUID, List<String>> explicitByServerId = explicitRepository.findByServerIdIn(serverIds).stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        DockerExpectedServiceEntity::getServerId,
                        java.util.stream.Collectors.mapping(DockerExpectedServiceEntity::getProfile, java.util.stream.Collectors.toList())
                ));

        Set<UUID> templateIds = specByServerId.values().stream()
                .filter(s -> s.getMode() == ExpectedSetMode.TEMPLATE)
                .map(DockerExpectedServiceSpecEntity::getTemplateId)
                .filter(id -> id != null)
                .collect(java.util.stream.Collectors.toSet());

        Map<UUID, List<String>> templateItemsById = templateIds.isEmpty()
                ? Map.of()
                : templateItemRepository.findByTemplateIdIn(List.copyOf(templateIds)).stream()
                        .collect(java.util.stream.Collectors.groupingBy(
                                ExpectedSetTemplateItemEntity::getTemplateId,
                                java.util.stream.Collectors.mapping(ExpectedSetTemplateItemEntity::getValue, java.util.stream.Collectors.toList())
                        ));

        List<DockerExpectedServicesSpecDto> result = new ArrayList<>();
        for (ServerEntity s : servers) {
            DockerExpectedServiceSpecEntity spec = specByServerId.get(s.getId());
            ExpectedSetMode mode = spec == null ? ExpectedSetMode.UNCONFIGURED : spec.getMode();
            UUID templateId = spec == null ? null : spec.getTemplateId();
            List<String> items;
            if (mode == ExpectedSetMode.TEMPLATE && templateId != null) {
                items = templateItemsById.getOrDefault(templateId, List.of());
            } else {
                items = explicitByServerId.getOrDefault(s.getId(), List.of());
            }
            result.add(new DockerExpectedServicesSpecDto(s.getId(), mode, templateId, List.copyOf(items)));
        }

        result.sort((a, b) -> {
            String as = serverById.get(a.serverId()) == null ? a.serverId().toString() : serverById.get(a.serverId()).getName();
            String bs = serverById.get(b.serverId()) == null ? b.serverId().toString() : serverById.get(b.serverId()).getName();
            return as.compareToIgnoreCase(bs);
        });
        return List.copyOf(result);
    }

    @Transactional
    public List<DockerExpectedServicesSpecDto> replace(UUID environmentId, DockerExpectedServicesSpecReplaceRequestDto request) {
        requireEnvironment(environmentId);
        validateReplaceRequest(request);

        List<ServerEntity> servers = serverRepository.findByEnvironmentId(environmentId);
        Map<UUID, ServerEntity> serverById = servers.stream()
                .collect(java.util.stream.Collectors.toMap(ServerEntity::getId, Function.identity()));
        List<UUID> serverIds = servers.stream().map(ServerEntity::getId).toList();

        List<DockerExpectedServicesSpecDto> specs = request.specs() == null ? List.of() : request.specs();

        Set<UUID> seen = new HashSet<>();
        Set<UUID> referencedTemplateIds = new HashSet<>();

        for (DockerExpectedServicesSpecDto s : specs) {
            if (s == null) throw new ResponseStatusException(BAD_REQUEST, "specs cannot contain null");
            if (s.serverId() == null) throw new ResponseStatusException(BAD_REQUEST, "serverId is required");
            if (!serverById.containsKey(s.serverId())) throw new ResponseStatusException(BAD_REQUEST, "serverId is not part of this environment: " + s.serverId());
            if (s.mode() == null) throw new ResponseStatusException(BAD_REQUEST, "mode is required");
            if (s.mode() == ExpectedSetMode.UNCONFIGURED) throw new ResponseStatusException(BAD_REQUEST, "mode cannot be UNCONFIGURED");

            if (!seen.add(s.serverId())) throw new ResponseStatusException(BAD_REQUEST, "Duplicate spec for serverId: " + s.serverId());

            if (s.mode() == ExpectedSetMode.TEMPLATE) {
                if (s.templateId() == null) throw new ResponseStatusException(BAD_REQUEST, "templateId is required for TEMPLATE mode");
                referencedTemplateIds.add(s.templateId());
                if (s.items() != null && !s.items().isEmpty()) {
                    throw new ResponseStatusException(BAD_REQUEST, "items must be empty in TEMPLATE mode");
                }
            } else if (s.mode() == ExpectedSetMode.EXPLICIT) {
                if (s.templateId() != null) throw new ResponseStatusException(BAD_REQUEST, "templateId must be null in EXPLICIT mode");
                validateProfiles(s.items() == null ? List.of() : s.items());
            }
        }

        if (!referencedTemplateIds.isEmpty()) {
            Map<UUID, ExpectedSetTemplateEntity> templates = templateRepository.findAllById(referencedTemplateIds).stream()
                    .collect(java.util.stream.Collectors.toMap(ExpectedSetTemplateEntity::getId, Function.identity()));
            for (UUID tid : referencedTemplateIds) {
                ExpectedSetTemplateEntity t = templates.get(tid);
                if (t == null) throw new ResponseStatusException(BAD_REQUEST, "Template not found: " + tid);
                if (t.getKind() != ExpectedSetTemplateKind.DOCKER_SERVICE_PROFILE) {
                    throw new ResponseStatusException(BAD_REQUEST, "Template kind mismatch (expected DOCKER_SERVICE_PROFILE): " + tid);
                }
            }
        }

        if (!serverIds.isEmpty()) {
            specRepository.deleteByServerIdIn(serverIds);
            explicitRepository.deleteByServerIdIn(serverIds);
        }

        Instant now = Instant.now();
        specRepository.saveAll(specs.stream()
                .map(s -> new DockerExpectedServiceSpecEntity(UUID.randomUUID(), s.serverId(), s.mode(), s.templateId(), now))
                .toList());

        List<DockerExpectedServiceEntity> explicitItems = new ArrayList<>();
        for (DockerExpectedServicesSpecDto s : specs) {
            if (s.mode() != ExpectedSetMode.EXPLICIT) continue;
            for (String raw : (s.items() == null ? List.<String>of() : s.items())) {
                String profile = raw == null ? "" : raw.trim();
                if (profile.isEmpty()) continue;
                explicitItems.add(new DockerExpectedServiceEntity(UUID.randomUUID(), s.serverId(), profile, now));
            }
        }
        explicitRepository.saveAll(explicitItems);

        return list(environmentId);
    }

    private void requireEnvironment(UUID environmentId) {
        if (!environmentRepository.existsById(environmentId)) {
            throw new ResponseStatusException(NOT_FOUND, "Environment not found");
        }
    }

    private static void validateReplaceRequest(DockerExpectedServicesSpecReplaceRequestDto request) {
        if (request == null) {
            throw new ResponseStatusException(BAD_REQUEST, "Request body is required");
        }
    }

    private static void validateProfiles(List<String> items) {
        Set<String> seen = new HashSet<>();
        for (String raw : items) {
            String v = raw == null ? "" : raw.trim();
            if (v.isEmpty()) {
                throw new ResponseStatusException(BAD_REQUEST, "items cannot contain empty values");
            }
            if (!seen.add(v)) {
                throw new ResponseStatusException(BAD_REQUEST, "Duplicate item: " + v);
            }
        }
    }
}

