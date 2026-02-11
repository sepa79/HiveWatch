package io.pockethive.hivewatch.service.environments.servers;

import io.pockethive.hivewatch.service.actuator.ActuatorTargetEntity;
import io.pockethive.hivewatch.service.actuator.ActuatorTargetRepository;
import io.pockethive.hivewatch.service.api.ServerCloneRequestDto;
import io.pockethive.hivewatch.service.api.ServerDto;
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
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class ServerCloneService {
    private final ServerRepository serverRepository;
    private final TomcatTargetRepository tomcatTargetRepository;
    private final ActuatorTargetRepository actuatorTargetRepository;
    private final TomcatExpectedWebappSpecRepository tomcatExpectedWebappSpecRepository;
    private final TomcatExpectedWebappRepository tomcatExpectedWebappRepository;
    private final DockerExpectedServiceSpecRepository dockerExpectedServiceSpecRepository;
    private final DockerExpectedServiceRepository dockerExpectedServiceRepository;

    public ServerCloneService(
            ServerRepository serverRepository,
            TomcatTargetRepository tomcatTargetRepository,
            ActuatorTargetRepository actuatorTargetRepository,
            TomcatExpectedWebappSpecRepository tomcatExpectedWebappSpecRepository,
            TomcatExpectedWebappRepository tomcatExpectedWebappRepository,
            DockerExpectedServiceSpecRepository dockerExpectedServiceSpecRepository,
            DockerExpectedServiceRepository dockerExpectedServiceRepository
    ) {
        this.serverRepository = serverRepository;
        this.tomcatTargetRepository = tomcatTargetRepository;
        this.actuatorTargetRepository = actuatorTargetRepository;
        this.tomcatExpectedWebappSpecRepository = tomcatExpectedWebappSpecRepository;
        this.tomcatExpectedWebappRepository = tomcatExpectedWebappRepository;
        this.dockerExpectedServiceSpecRepository = dockerExpectedServiceSpecRepository;
        this.dockerExpectedServiceRepository = dockerExpectedServiceRepository;
    }

    @Transactional
    public ServerDto cloneServer(UUID environmentId, UUID sourceServerId, ServerCloneRequestDto request) {
        if (environmentId == null) throw new ResponseStatusException(BAD_REQUEST, "environmentId is required");
        if (sourceServerId == null) throw new ResponseStatusException(BAD_REQUEST, "serverId is required");
        validateRequest(request);

        ServerEntity source = serverRepository.findById(sourceServerId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Server not found"));
        if (!source.getEnvironmentId().equals(environmentId)) {
            throw new ResponseStatusException(NOT_FOUND, "Server not found");
        }

        String name = request.name().trim();
        if (serverRepository.existsByEnvironmentIdAndName(environmentId, name)) {
            throw new ResponseStatusException(BAD_REQUEST, "Server name already exists in this environment");
        }

        Instant now = Instant.now();
        UUID newServerId = UUID.randomUUID();
        ServerEntity created = serverRepository.save(new ServerEntity(newServerId, environmentId, name, now));

        List<TomcatTargetEntity> sourceTomcats = tomcatTargetRepository.findByServerIdIn(List.of(sourceServerId));
        tomcatTargetRepository.saveAll(sourceTomcats.stream()
                .map(t -> new TomcatTargetEntity(
                        UUID.randomUUID(),
                        newServerId,
                        t.getRole(),
                        t.getBaseUrl(),
                        t.getPort(),
                        t.getUsername(),
                        t.getPassword(),
                        t.getConnectTimeoutMs(),
                        t.getRequestTimeoutMs(),
                        now
                ))
                .toList());

        List<ActuatorTargetEntity> sourceActuators = actuatorTargetRepository.findByServerIdIn(List.of(sourceServerId));
        actuatorTargetRepository.saveAll(sourceActuators.stream()
                .map(t -> new ActuatorTargetEntity(
                        UUID.randomUUID(),
                        newServerId,
                        t.getRole(),
                        t.getBaseUrl(),
                        t.getPort(),
                        t.getProfile(),
                        t.getConnectTimeoutMs(),
                        t.getRequestTimeoutMs(),
                        now
                ))
                .toList());

        List<TomcatExpectedWebappSpecEntity> sourceTomcatSpecs = tomcatExpectedWebappSpecRepository.findByServerIdIn(List.of(sourceServerId));
        tomcatExpectedWebappSpecRepository.saveAll(sourceTomcatSpecs.stream()
                .map(s -> new TomcatExpectedWebappSpecEntity(
                        UUID.randomUUID(),
                        newServerId,
                        s.getRole(),
                        s.getMode(),
                        s.getTemplateId(),
                        now
                ))
                .toList());

        List<TomcatExpectedWebappEntity> sourceTomcatItems = tomcatExpectedWebappRepository.findByServerIdIn(List.of(sourceServerId));
        tomcatExpectedWebappRepository.saveAll(sourceTomcatItems.stream()
                .map(i -> new TomcatExpectedWebappEntity(
                        UUID.randomUUID(),
                        newServerId,
                        i.getRole(),
                        i.getPath(),
                        now
                ))
                .toList());

        List<DockerExpectedServiceSpecEntity> sourceDockerSpecs = dockerExpectedServiceSpecRepository.findByServerIdIn(List.of(sourceServerId));
        dockerExpectedServiceSpecRepository.saveAll(sourceDockerSpecs.stream()
                .map(s -> new DockerExpectedServiceSpecEntity(
                        UUID.randomUUID(),
                        newServerId,
                        s.getMode(),
                        s.getTemplateId(),
                        now
                ))
                .toList());

        List<DockerExpectedServiceEntity> sourceDockerItems = dockerExpectedServiceRepository.findByServerIdIn(List.of(sourceServerId));
        dockerExpectedServiceRepository.saveAll(sourceDockerItems.stream()
                .map(i -> new DockerExpectedServiceEntity(
                        UUID.randomUUID(),
                        newServerId,
                        i.getProfile(),
                        now
                ))
                .toList());

        return ServerService.toDto(created);
    }

    private static void validateRequest(ServerCloneRequestDto request) {
        if (request == null) {
            throw new ResponseStatusException(BAD_REQUEST, "Request body is required");
        }
        if (request.name() == null || request.name().trim().isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "name is required");
        }
    }
}

