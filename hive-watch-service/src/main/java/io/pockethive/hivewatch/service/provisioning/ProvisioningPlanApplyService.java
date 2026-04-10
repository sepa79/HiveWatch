package io.pockethive.hivewatch.service.provisioning;

import io.pockethive.hivewatch.service.actuator.ActuatorTargetEntity;
import io.pockethive.hivewatch.service.actuator.ActuatorTargetRepository;
import io.pockethive.hivewatch.service.actuator.ActuatorTargetValidation;
import io.pockethive.hivewatch.service.api.ProvisioningActuatorTargetDto;
import io.pockethive.hivewatch.service.api.ProvisioningAppliedObjectDto;
import io.pockethive.hivewatch.service.api.ProvisioningApplySummaryDto;
import io.pockethive.hivewatch.service.api.ProvisioningChangeModeDto;
import io.pockethive.hivewatch.service.api.ProvisioningPlanApplyRequestDto;
import io.pockethive.hivewatch.service.api.ProvisioningPlanApplyResultDto;
import io.pockethive.hivewatch.service.api.ProvisioningPlanObjectTypeDto;
import io.pockethive.hivewatch.service.api.ProvisioningPlanValidationResultDto;
import io.pockethive.hivewatch.service.api.ProvisioningServerDto;
import io.pockethive.hivewatch.service.api.ProvisioningTomcatTargetDto;
import io.pockethive.hivewatch.service.environments.EnvironmentEntity;
import io.pockethive.hivewatch.service.environments.EnvironmentRepository;
import io.pockethive.hivewatch.service.environments.servers.ServerEntity;
import io.pockethive.hivewatch.service.environments.servers.ServerRepository;
import io.pockethive.hivewatch.service.targets.TargetConnectionValidation;
import io.pockethive.hivewatch.service.tomcat.TomcatTargetEntity;
import io.pockethive.hivewatch.service.tomcat.TomcatTargetRepository;
import io.pockethive.hivewatch.service.tomcat.TomcatTargetValidation;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class ProvisioningPlanApplyService {
    private final ProvisioningPlanValidationService validationService;
    private final EnvironmentRepository environmentRepository;
    private final ServerRepository serverRepository;
    private final TomcatTargetRepository tomcatTargetRepository;
    private final ActuatorTargetRepository actuatorTargetRepository;

    public ProvisioningPlanApplyService(
            ProvisioningPlanValidationService validationService,
            EnvironmentRepository environmentRepository,
            ServerRepository serverRepository,
            TomcatTargetRepository tomcatTargetRepository,
            ActuatorTargetRepository actuatorTargetRepository
    ) {
        this.validationService = validationService;
        this.environmentRepository = environmentRepository;
        this.serverRepository = serverRepository;
        this.tomcatTargetRepository = tomcatTargetRepository;
        this.actuatorTargetRepository = actuatorTargetRepository;
    }

    @Transactional
    public ProvisioningPlanApplyResultDto apply(ProvisioningPlanApplyRequestDto request) {
        if (request == null) {
            throw new ResponseStatusException(BAD_REQUEST, "Request body is required");
        }
        if (request.scanAfterApply()) {
            throw new ResponseStatusException(BAD_REQUEST, "scanAfterApply is not supported yet");
        }

        ProvisioningPlanValidationResultDto validation = validationService.validate(request.plan());
        if (!validation.valid()) {
            throw new ResponseStatusException(BAD_REQUEST, "Provisioning plan is invalid; call validate for details");
        }

        List<ProvisioningAppliedObjectDto> objects = new ArrayList<>();
        Counter counter = new Counter();
        EnvironmentEntity environment = applyEnvironment(request, objects, counter);

        for (ProvisioningServerDto server : request.plan().servers()) {
            ServerEntity savedServer = applyServer(environment.getId(), server, objects, counter);
            for (ProvisioningTomcatTargetDto target : server.tomcatTargets()) {
                applyTomcatTarget(savedServer.getId(), server.clientRef(), target, objects, counter);
            }
            for (ProvisioningActuatorTargetDto target : server.actuatorTargets()) {
                applyActuatorTarget(savedServer.getId(), server.clientRef(), target, objects, counter);
            }
        }

        return new ProvisioningPlanApplyResultDto(
                environment.getId(),
                new ProvisioningApplySummaryDto(
                        counter.environmentsCreated,
                        counter.serversCreated,
                        counter.tomcatTargetsCreated,
                        counter.actuatorTargetsCreated
                ),
                List.copyOf(objects),
                validation
        );
    }

    private EnvironmentEntity applyEnvironment(
            ProvisioningPlanApplyRequestDto request,
            List<ProvisioningAppliedObjectDto> objects,
            Counter counter
    ) {
        if (request.plan().environment().mode() == ProvisioningChangeModeDto.CREATE) {
            EnvironmentEntity saved = environmentRepository.save(new EnvironmentEntity(
                    UUID.randomUUID(),
                    request.plan().environment().name().trim()
            ));
            counter.environmentsCreated++;
            objects.add(new ProvisioningAppliedObjectDto(
                    ProvisioningPlanObjectTypeDto.ENVIRONMENT,
                    null,
                    saved.getId(),
                    saved.getName()
            ));
            return saved;
        }

        EnvironmentEntity existing = environmentRepository.findById(request.plan().environment().environmentId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Environment not found"));
        objects.add(new ProvisioningAppliedObjectDto(
                ProvisioningPlanObjectTypeDto.ENVIRONMENT,
                existing.getId().toString(),
                existing.getId(),
                existing.getName()
        ));
        return existing;
    }

    private ServerEntity applyServer(
            UUID environmentId,
            ProvisioningServerDto server,
            List<ProvisioningAppliedObjectDto> objects,
            Counter counter
    ) {
        String clientRef = server.clientRef().trim();
        if (server.mode() == ProvisioningChangeModeDto.CREATE) {
            ServerEntity saved = serverRepository.save(new ServerEntity(
                    UUID.randomUUID(),
                    environmentId,
                    server.name().trim(),
                    Instant.now()
            ));
            counter.serversCreated++;
            objects.add(new ProvisioningAppliedObjectDto(
                    ProvisioningPlanObjectTypeDto.SERVER,
                    clientRef,
                    saved.getId(),
                    saved.getName()
            ));
            return saved;
        }

        ServerEntity existing = serverRepository.findById(server.serverId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Server not found"));
        objects.add(new ProvisioningAppliedObjectDto(
                ProvisioningPlanObjectTypeDto.SERVER,
                clientRef,
                existing.getId(),
                existing.getName()
        ));
        return existing;
    }

    private void applyTomcatTarget(
            UUID serverId,
            String serverClientRef,
            ProvisioningTomcatTargetDto target,
            List<ProvisioningAppliedObjectDto> objects,
            Counter counter
    ) {
        TomcatTargetEntity saved = tomcatTargetRepository.save(new TomcatTargetEntity(
                UUID.randomUUID(),
                serverId,
                target.role(),
                TomcatTargetValidation.parseBaseUrl(target.baseUrl()).toString(),
                target.port(),
                TomcatTargetValidation.sanitizeUsername(target.username()),
                TomcatTargetValidation.requirePassword(target.password()),
                target.connectTimeoutMs(),
                target.requestTimeoutMs(),
                Instant.now()
        ));
        TargetConnectionValidation.validateTimeouts(saved.getConnectTimeoutMs(), saved.getRequestTimeoutMs());
        counter.tomcatTargetsCreated++;
        objects.add(new ProvisioningAppliedObjectDto(
                ProvisioningPlanObjectTypeDto.TOMCAT_TARGET,
                serverClientRef,
                saved.getId(),
                "TOMCAT_MANAGER_HTML " + saved.getRole() + " " + saved.getBaseUrl() + ":" + saved.getPort()
        ));
    }

    private void applyActuatorTarget(
            UUID serverId,
            String serverClientRef,
            ProvisioningActuatorTargetDto target,
            List<ProvisioningAppliedObjectDto> objects,
            Counter counter
    ) {
        ActuatorTargetEntity saved = actuatorTargetRepository.save(new ActuatorTargetEntity(
                UUID.randomUUID(),
                serverId,
                target.role(),
                ActuatorTargetValidation.parseBaseUrl(target.baseUrl()).toString(),
                target.port(),
                ActuatorTargetValidation.sanitizeProfile(target.profile()),
                target.connectTimeoutMs(),
                target.requestTimeoutMs(),
                Instant.now()
        ));
        TargetConnectionValidation.validateTimeouts(saved.getConnectTimeoutMs(), saved.getRequestTimeoutMs());
        counter.actuatorTargetsCreated++;
        objects.add(new ProvisioningAppliedObjectDto(
                ProvisioningPlanObjectTypeDto.ACTUATOR_TARGET,
                serverClientRef,
                saved.getId(),
                "ACTUATOR_HTTP " + saved.getRole() + " " + saved.getBaseUrl() + ":" + saved.getPort()
        ));
    }

    private static final class Counter {
        int environmentsCreated;
        int serversCreated;
        int tomcatTargetsCreated;
        int actuatorTargetsCreated;
    }
}
