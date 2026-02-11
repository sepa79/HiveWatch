package io.pockethive.hivewatch.service.environments;

import io.pockethive.hivewatch.service.actuator.ActuatorTargetEntity;
import io.pockethive.hivewatch.service.actuator.ActuatorTargetRepository;
import io.pockethive.hivewatch.service.api.EnvironmentCloneResultDto;
import io.pockethive.hivewatch.service.environments.servers.ServerEntity;
import io.pockethive.hivewatch.service.environments.servers.ServerRepository;
import io.pockethive.hivewatch.service.expectedsets.docker.DockerExpectedServiceEntity;
import io.pockethive.hivewatch.service.expectedsets.docker.DockerExpectedServiceRepository;
import io.pockethive.hivewatch.service.expectedsets.docker.DockerExpectedServiceSpecEntity;
import io.pockethive.hivewatch.service.expectedsets.docker.DockerExpectedServiceSpecRepository;
import io.pockethive.hivewatch.service.expectedsets.tomcat.TomcatExpectedWebappSpecEntity;
import io.pockethive.hivewatch.service.expectedsets.tomcat.TomcatExpectedWebappSpecRepository;
import io.pockethive.hivewatch.service.tomcat.TomcatTargetEntity;
import io.pockethive.hivewatch.service.tomcat.TomcatTargetRepository;
import io.pockethive.hivewatch.service.tomcat.expected.TomcatExpectedWebappEntity;
import io.pockethive.hivewatch.service.tomcat.expected.TomcatExpectedWebappRepository;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class EnvironmentCloneService {
    private final EnvironmentRepository environmentRepository;
    private final ServerRepository serverRepository;
    private final TomcatTargetRepository tomcatTargetRepository;
    private final ActuatorTargetRepository actuatorTargetRepository;
    private final TomcatExpectedWebappSpecRepository tomcatExpectedWebappSpecRepository;
    private final TomcatExpectedWebappRepository tomcatExpectedWebappRepository;
    private final DockerExpectedServiceSpecRepository dockerExpectedServiceSpecRepository;
    private final DockerExpectedServiceRepository dockerExpectedServiceRepository;

    public EnvironmentCloneService(
            EnvironmentRepository environmentRepository,
            ServerRepository serverRepository,
            TomcatTargetRepository tomcatTargetRepository,
            ActuatorTargetRepository actuatorTargetRepository,
            TomcatExpectedWebappSpecRepository tomcatExpectedWebappSpecRepository,
            TomcatExpectedWebappRepository tomcatExpectedWebappRepository,
            DockerExpectedServiceSpecRepository dockerExpectedServiceSpecRepository,
            DockerExpectedServiceRepository dockerExpectedServiceRepository
    ) {
        this.environmentRepository = environmentRepository;
        this.serverRepository = serverRepository;
        this.tomcatTargetRepository = tomcatTargetRepository;
        this.actuatorTargetRepository = actuatorTargetRepository;
        this.tomcatExpectedWebappSpecRepository = tomcatExpectedWebappSpecRepository;
        this.tomcatExpectedWebappRepository = tomcatExpectedWebappRepository;
        this.dockerExpectedServiceSpecRepository = dockerExpectedServiceSpecRepository;
        this.dockerExpectedServiceRepository = dockerExpectedServiceRepository;
    }

    @Transactional
    public EnvironmentCloneResultDto cloneConfig(UUID targetEnvironmentId, UUID sourceEnvironmentId) {
        if (targetEnvironmentId == null) throw new ResponseStatusException(BAD_REQUEST, "targetEnvironmentId is required");
        if (sourceEnvironmentId == null) throw new ResponseStatusException(BAD_REQUEST, "sourceEnvironmentId is required");
        if (targetEnvironmentId.equals(sourceEnvironmentId)) {
            throw new ResponseStatusException(BAD_REQUEST, "sourceEnvironmentId must be different than target environment");
        }

        if (!environmentRepository.existsById(targetEnvironmentId)) {
            throw new ResponseStatusException(NOT_FOUND, "Target environment not found");
        }
        if (!environmentRepository.existsById(sourceEnvironmentId)) {
            throw new ResponseStatusException(NOT_FOUND, "Source environment not found");
        }

        if (!serverRepository.findByEnvironmentId(targetEnvironmentId).isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "Target environment must be empty to clone config");
        }

        List<ServerEntity> sourceServers = serverRepository.findByEnvironmentId(sourceEnvironmentId);
        List<UUID> sourceServerIds = sourceServers.stream().map(ServerEntity::getId).toList();

        Instant now = Instant.now();
        Map<UUID, UUID> serverIdMap = new HashMap<>();
        List<ServerEntity> clonedServers = sourceServers.stream()
                .map(s -> {
                    UUID newId = UUID.randomUUID();
                    serverIdMap.put(s.getId(), newId);
                    return new ServerEntity(newId, targetEnvironmentId, s.getName(), now);
                })
                .toList();
        serverRepository.saveAll(clonedServers);

        List<TomcatTargetEntity> sourceTomcats = sourceServerIds.isEmpty() ? List.of() : tomcatTargetRepository.findByServerIdIn(sourceServerIds);
        List<TomcatTargetEntity> clonedTomcats = sourceTomcats.stream()
                .map(t -> new TomcatTargetEntity(
                        UUID.randomUUID(),
                        serverIdMap.get(t.getServerId()),
                        t.getRole(),
                        t.getBaseUrl(),
                        t.getPort(),
                        t.getUsername(),
                        t.getPassword(),
                        t.getConnectTimeoutMs(),
                        t.getRequestTimeoutMs(),
                        now
                ))
                .toList();
        tomcatTargetRepository.saveAll(clonedTomcats);

        List<ActuatorTargetEntity> sourceActuators = sourceServerIds.isEmpty() ? List.of() : actuatorTargetRepository.findByServerIdIn(sourceServerIds);
        List<ActuatorTargetEntity> clonedActuators = sourceActuators.stream()
                .map(t -> new ActuatorTargetEntity(
                        UUID.randomUUID(),
                        serverIdMap.get(t.getServerId()),
                        t.getRole(),
                        t.getBaseUrl(),
                        t.getPort(),
                        t.getProfile(),
                        t.getConnectTimeoutMs(),
                        t.getRequestTimeoutMs(),
                        now
                ))
                .toList();
        actuatorTargetRepository.saveAll(clonedActuators);

        List<TomcatExpectedWebappSpecEntity> sourceTomcatSpecs = sourceServerIds.isEmpty() ? List.of() : tomcatExpectedWebappSpecRepository.findByServerIdIn(sourceServerIds);
        List<TomcatExpectedWebappSpecEntity> clonedTomcatSpecs = sourceTomcatSpecs.stream()
                .map(s -> new TomcatExpectedWebappSpecEntity(
                        UUID.randomUUID(),
                        serverIdMap.get(s.getServerId()),
                        s.getRole(),
                        s.getMode(),
                        s.getTemplateId(),
                        now
                ))
                .toList();
        tomcatExpectedWebappSpecRepository.saveAll(clonedTomcatSpecs);

        List<TomcatExpectedWebappEntity> sourceTomcatItems = sourceServerIds.isEmpty() ? List.of() : tomcatExpectedWebappRepository.findByServerIdIn(sourceServerIds);
        List<TomcatExpectedWebappEntity> clonedTomcatItems = sourceTomcatItems.stream()
                .map(i -> new TomcatExpectedWebappEntity(
                        UUID.randomUUID(),
                        serverIdMap.get(i.getServerId()),
                        i.getRole(),
                        i.getPath(),
                        now
                ))
                .toList();
        tomcatExpectedWebappRepository.saveAll(clonedTomcatItems);

        List<DockerExpectedServiceSpecEntity> sourceDockerSpecs = sourceServerIds.isEmpty() ? List.of() : dockerExpectedServiceSpecRepository.findByServerIdIn(sourceServerIds);
        List<DockerExpectedServiceSpecEntity> clonedDockerSpecs = sourceDockerSpecs.stream()
                .map(s -> new DockerExpectedServiceSpecEntity(
                        UUID.randomUUID(),
                        serverIdMap.get(s.getServerId()),
                        s.getMode(),
                        s.getTemplateId(),
                        now
                ))
                .toList();
        dockerExpectedServiceSpecRepository.saveAll(clonedDockerSpecs);

        List<DockerExpectedServiceEntity> sourceDockerItems = sourceServerIds.isEmpty() ? List.of() : dockerExpectedServiceRepository.findByServerIdIn(sourceServerIds);
        List<DockerExpectedServiceEntity> clonedDockerItems = sourceDockerItems.stream()
                .map(i -> new DockerExpectedServiceEntity(
                        UUID.randomUUID(),
                        serverIdMap.get(i.getServerId()),
                        i.getProfile(),
                        now
                ))
                .toList();
        dockerExpectedServiceRepository.saveAll(clonedDockerItems);

        return new EnvironmentCloneResultDto(
                clonedServers.size(),
                clonedTomcats.size(),
                clonedActuators.size(),
                clonedTomcatSpecs.size(),
                clonedTomcatItems.size(),
                clonedDockerSpecs.size(),
                clonedDockerItems.size()
        );
    }
}

