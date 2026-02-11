package io.pockethive.hivewatch.service.expectedsets;

import io.pockethive.hivewatch.service.api.ExpectedSetMode;
import io.pockethive.hivewatch.service.api.ExpectedSetTemplateKind;
import io.pockethive.hivewatch.service.api.TomcatExpectedWebappsSpecDto;
import io.pockethive.hivewatch.service.api.TomcatExpectedWebappsSpecReplaceRequestDto;
import io.pockethive.hivewatch.service.api.TomcatRole;
import io.pockethive.hivewatch.service.environments.EnvironmentRepository;
import io.pockethive.hivewatch.service.environments.servers.ServerEntity;
import io.pockethive.hivewatch.service.environments.servers.ServerRepository;
import io.pockethive.hivewatch.service.expectedsets.tomcat.TomcatExpectedWebappSpecEntity;
import io.pockethive.hivewatch.service.expectedsets.tomcat.TomcatExpectedWebappSpecRepository;
import io.pockethive.hivewatch.service.tomcat.TomcatTargetEntity;
import io.pockethive.hivewatch.service.tomcat.TomcatTargetRepository;
import io.pockethive.hivewatch.service.tomcat.expected.TomcatExpectedWebappEntity;
import io.pockethive.hivewatch.service.tomcat.expected.TomcatExpectedWebappRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class TomcatExpectedWebappsSpecService {
    private static final Set<String> BUILT_IN_WEBAPPS = Set.of("/", "/manager", "/host-manager", "/docs", "/examples");

    private final EnvironmentRepository environmentRepository;
    private final ServerRepository serverRepository;
    private final TomcatTargetRepository tomcatTargetRepository;
    private final TomcatExpectedWebappRepository explicitRepository;
    private final TomcatExpectedWebappSpecRepository specRepository;
    private final ExpectedSetTemplateRepository templateRepository;
    private final ExpectedSetTemplateItemRepository templateItemRepository;

    public TomcatExpectedWebappsSpecService(
            EnvironmentRepository environmentRepository,
            ServerRepository serverRepository,
            TomcatTargetRepository tomcatTargetRepository,
            TomcatExpectedWebappRepository explicitRepository,
            TomcatExpectedWebappSpecRepository specRepository,
            ExpectedSetTemplateRepository templateRepository,
            ExpectedSetTemplateItemRepository templateItemRepository
    ) {
        this.environmentRepository = environmentRepository;
        this.serverRepository = serverRepository;
        this.tomcatTargetRepository = tomcatTargetRepository;
        this.explicitRepository = explicitRepository;
        this.specRepository = specRepository;
        this.templateRepository = templateRepository;
        this.templateItemRepository = templateItemRepository;
    }

    @Transactional(readOnly = true)
    public List<TomcatExpectedWebappsSpecDto> list(UUID environmentId) {
        requireEnvironment(environmentId);

        List<ServerEntity> servers = serverRepository.findByEnvironmentId(environmentId);
        if (servers.isEmpty()) return List.of();

        Map<UUID, ServerEntity> serverById = servers.stream()
                .collect(java.util.stream.Collectors.toMap(ServerEntity::getId, Function.identity()));

        List<UUID> serverIds = servers.stream().map(ServerEntity::getId).toList();
        List<TomcatTargetEntity> targets = tomcatTargetRepository.findByServerIdIn(serverIds);
        if (targets.isEmpty()) return List.of();

        List<Key> keys = targets.stream()
                .map(t -> new Key(t.getServerId(), t.getRole()))
                .distinct()
                .toList();

        Map<Key, TomcatExpectedWebappSpecEntity> specByKey = specRepository.findByServerIdIn(serverIds).stream()
                .collect(java.util.stream.Collectors.toMap(e -> new Key(e.getServerId(), e.getRole()), Function.identity(), (a, b) -> a));

        Map<Key, List<String>> explicitByKey = explicitRepository.findByServerIdIn(serverIds).stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        e -> new Key(e.getServerId(), e.getRole()),
                        java.util.stream.Collectors.mapping(TomcatExpectedWebappEntity::getPath, java.util.stream.Collectors.toList())
                ));

        Set<UUID> templateIds = specByKey.values().stream()
                .filter(s -> s.getMode() == ExpectedSetMode.TEMPLATE)
                .map(TomcatExpectedWebappSpecEntity::getTemplateId)
                .filter(id -> id != null)
                .collect(java.util.stream.Collectors.toSet());

        Map<UUID, List<String>> templateItemsById = templateIds.isEmpty()
                ? Map.of()
                : templateItemRepository.findByTemplateIdIn(List.copyOf(templateIds)).stream()
                        .collect(java.util.stream.Collectors.groupingBy(
                                ExpectedSetTemplateItemEntity::getTemplateId,
                                java.util.stream.Collectors.mapping(ExpectedSetTemplateItemEntity::getValue, java.util.stream.Collectors.toList())
                        ));

        List<TomcatExpectedWebappsSpecDto> result = new ArrayList<>();
        for (Key k : keys) {
            TomcatExpectedWebappSpecEntity spec = specByKey.get(k);
            ExpectedSetMode mode = spec == null ? ExpectedSetMode.UNCONFIGURED : spec.getMode();
            UUID templateId = spec == null ? null : spec.getTemplateId();

            List<String> items;
            if (mode == ExpectedSetMode.TEMPLATE && templateId != null) {
                items = templateItemsById.getOrDefault(templateId, List.of());
            } else {
                items = explicitByKey.getOrDefault(k, List.of());
            }

            result.add(new TomcatExpectedWebappsSpecDto(k.serverId(), k.role(), mode, templateId, List.copyOf(items)));
        }

        result.sort((a, b) -> {
            String as = serverById.get(a.serverId()) == null ? a.serverId().toString() : serverById.get(a.serverId()).getName();
            String bs = serverById.get(b.serverId()) == null ? b.serverId().toString() : serverById.get(b.serverId()).getName();
            int byServer = as.compareToIgnoreCase(bs);
            if (byServer != 0) return byServer;
            return a.role().compareTo(b.role());
        });
        return List.copyOf(result);
    }

    @Transactional
    public List<TomcatExpectedWebappsSpecDto> replace(UUID environmentId, TomcatExpectedWebappsSpecReplaceRequestDto request) {
        requireEnvironment(environmentId);
        validateReplaceRequest(request);

        List<ServerEntity> servers = serverRepository.findByEnvironmentId(environmentId);
        Map<UUID, ServerEntity> serverById = servers.stream()
                .collect(java.util.stream.Collectors.toMap(ServerEntity::getId, Function.identity()));
        List<UUID> serverIds = servers.stream().map(ServerEntity::getId).toList();

        List<TomcatExpectedWebappsSpecDto> specs = request.specs() == null ? List.of() : request.specs();

        Set<Key> seen = new HashSet<>();
        Set<UUID> referencedTemplateIds = new HashSet<>();

        for (TomcatExpectedWebappsSpecDto s : specs) {
            if (s == null) throw new ResponseStatusException(BAD_REQUEST, "specs cannot contain null");
            if (s.serverId() == null) throw new ResponseStatusException(BAD_REQUEST, "serverId is required");
            if (!serverById.containsKey(s.serverId())) throw new ResponseStatusException(BAD_REQUEST, "serverId is not part of this environment: " + s.serverId());
            if (s.role() == null) throw new ResponseStatusException(BAD_REQUEST, "role is required");
            if (s.mode() == null) throw new ResponseStatusException(BAD_REQUEST, "mode is required");
            if (s.mode() == ExpectedSetMode.UNCONFIGURED) throw new ResponseStatusException(BAD_REQUEST, "mode cannot be UNCONFIGURED");

            Key key = new Key(s.serverId(), s.role());
            if (!seen.add(key)) throw new ResponseStatusException(BAD_REQUEST, "Duplicate spec for serverId+role: " + s.serverId() + " " + s.role());

            if (s.mode() == ExpectedSetMode.TEMPLATE) {
                if (s.templateId() == null) throw new ResponseStatusException(BAD_REQUEST, "templateId is required for TEMPLATE mode");
                referencedTemplateIds.add(s.templateId());
                if (s.items() != null && !s.items().isEmpty()) {
                    throw new ResponseStatusException(BAD_REQUEST, "items must be empty in TEMPLATE mode");
                }
            } else if (s.mode() == ExpectedSetMode.EXPLICIT) {
                if (s.templateId() != null) throw new ResponseStatusException(BAD_REQUEST, "templateId must be null in EXPLICIT mode");
                validateWebappPaths(s.items() == null ? List.of() : s.items());
            }
        }

        if (!referencedTemplateIds.isEmpty()) {
            Map<UUID, ExpectedSetTemplateEntity> templates = templateRepository.findAllById(referencedTemplateIds).stream()
                    .collect(java.util.stream.Collectors.toMap(ExpectedSetTemplateEntity::getId, Function.identity()));
            for (UUID tid : referencedTemplateIds) {
                ExpectedSetTemplateEntity t = templates.get(tid);
                if (t == null) throw new ResponseStatusException(BAD_REQUEST, "Template not found: " + tid);
                if (t.getKind() != ExpectedSetTemplateKind.TOMCAT_WEBAPP_PATH) {
                    throw new ResponseStatusException(BAD_REQUEST, "Template kind mismatch (expected TOMCAT_WEBAPP_PATH): " + tid);
                }
            }
        }

        if (!serverIds.isEmpty()) {
            specRepository.deleteByServerIdIn(serverIds);
            explicitRepository.deleteByServerIdIn(serverIds);
        }

        Instant now = Instant.now();
        specRepository.saveAll(specs.stream()
                .map(s -> new TomcatExpectedWebappSpecEntity(UUID.randomUUID(), s.serverId(), s.role(), s.mode(), s.templateId(), now))
                .toList());

        List<TomcatExpectedWebappEntity> explicitItems = new ArrayList<>();
        for (TomcatExpectedWebappsSpecDto s : specs) {
            if (s.mode() != ExpectedSetMode.EXPLICIT) continue;
            for (String raw : (s.items() == null ? List.<String>of() : s.items())) {
                String path = raw == null ? "" : raw.trim();
                if (path.isEmpty()) continue;
                explicitItems.add(new TomcatExpectedWebappEntity(UUID.randomUUID(), s.serverId(), s.role(), path, now));
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

    private static void validateReplaceRequest(TomcatExpectedWebappsSpecReplaceRequestDto request) {
        if (request == null) {
            throw new ResponseStatusException(BAD_REQUEST, "Request body is required");
        }
    }

    private static void validateWebappPaths(List<String> items) {
        Set<String> seen = new HashSet<>();
        for (String raw : items) {
            String path = raw == null ? "" : raw.trim();
            if (path.isEmpty()) {
                throw new ResponseStatusException(BAD_REQUEST, "items cannot contain empty values");
            }
            if (!path.startsWith("/")) {
                throw new ResponseStatusException(BAD_REQUEST, "Webapp path must start with '/': " + path);
            }
            if (BUILT_IN_WEBAPPS.contains(path)) {
                throw new ResponseStatusException(BAD_REQUEST, "Built-in Tomcat webapp is not allowed in expected list: " + path);
            }
            if (!seen.add(path)) {
                throw new ResponseStatusException(BAD_REQUEST, "Duplicate item: " + path);
            }
        }
    }

    private record Key(UUID serverId, TomcatRole role) {
    }
}

