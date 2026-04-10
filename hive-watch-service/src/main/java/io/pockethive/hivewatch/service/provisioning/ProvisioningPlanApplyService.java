package io.pockethive.hivewatch.service.provisioning;

import io.pockethive.hivewatch.service.actuator.ActuatorTargetEntity;
import io.pockethive.hivewatch.service.actuator.ActuatorTargetRepository;
import io.pockethive.hivewatch.service.actuator.ActuatorTargetValidation;
import io.pockethive.hivewatch.service.api.DockerExpectedServicesSpecDto;
import io.pockethive.hivewatch.service.api.DockerExpectedServicesSpecReplaceRequestDto;
import io.pockethive.hivewatch.service.api.ExpectedSetMode;
import io.pockethive.hivewatch.service.api.ProvisioningActuatorTargetDto;
import io.pockethive.hivewatch.service.api.ProvisioningAppliedObjectDto;
import io.pockethive.hivewatch.service.api.ProvisioningApplySummaryDto;
import io.pockethive.hivewatch.service.api.ProvisioningChangeModeDto;
import io.pockethive.hivewatch.service.api.ProvisioningDockerExpectedServicesSpecDto;
import io.pockethive.hivewatch.service.api.ProvisioningExpectedSetChangeModeDto;
import io.pockethive.hivewatch.service.api.ProvisioningPlanApplyRequestDto;
import io.pockethive.hivewatch.service.api.ProvisioningPlanApplyResultDto;
import io.pockethive.hivewatch.service.api.ProvisioningPlanObjectTypeDto;
import io.pockethive.hivewatch.service.api.ProvisioningPlanValidationResultDto;
import io.pockethive.hivewatch.service.api.ProvisioningServerDto;
import io.pockethive.hivewatch.service.api.ProvisioningTomcatExpectedWebappsSpecDto;
import io.pockethive.hivewatch.service.api.ProvisioningTomcatTargetDto;
import io.pockethive.hivewatch.service.api.TomcatExpectedWebappsSpecDto;
import io.pockethive.hivewatch.service.api.TomcatExpectedWebappsSpecReplaceRequestDto;
import io.pockethive.hivewatch.service.audit.ConfigAuditService;
import io.pockethive.hivewatch.service.environments.EnvironmentEntity;
import io.pockethive.hivewatch.service.environments.EnvironmentRepository;
import io.pockethive.hivewatch.service.environments.servers.ServerEntity;
import io.pockethive.hivewatch.service.environments.servers.ServerRepository;
import io.pockethive.hivewatch.service.expectedsets.DockerExpectedServicesSpecService;
import io.pockethive.hivewatch.service.expectedsets.TomcatExpectedWebappsSpecService;
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
    private final TomcatExpectedWebappsSpecService tomcatExpectedWebappsSpecService;
    private final DockerExpectedServicesSpecService dockerExpectedServicesSpecService;
    private final ConfigAuditService configAuditService;

    public ProvisioningPlanApplyService(
            ProvisioningPlanValidationService validationService,
            EnvironmentRepository environmentRepository,
            ServerRepository serverRepository,
            TomcatTargetRepository tomcatTargetRepository,
            ActuatorTargetRepository actuatorTargetRepository,
            TomcatExpectedWebappsSpecService tomcatExpectedWebappsSpecService,
            DockerExpectedServicesSpecService dockerExpectedServicesSpecService,
            ConfigAuditService configAuditService
    ) {
        this.validationService = validationService;
        this.environmentRepository = environmentRepository;
        this.serverRepository = serverRepository;
        this.tomcatTargetRepository = tomcatTargetRepository;
        this.actuatorTargetRepository = actuatorTargetRepository;
        this.tomcatExpectedWebappsSpecService = tomcatExpectedWebappsSpecService;
        this.dockerExpectedServicesSpecService = dockerExpectedServicesSpecService;
        this.configAuditService = configAuditService;
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
            applyTomcatExpectedWebapps(environment.getId(), savedServer.getId(), server, objects, counter);
            applyDockerExpectedServices(environment.getId(), savedServer.getId(), server, objects, counter);
        }

        ProvisioningApplySummaryDto summary = new ProvisioningApplySummaryDto(
                counter.environmentsCreated,
                counter.serversCreated,
                counter.tomcatTargetsCreated,
                counter.actuatorTargetsCreated,
                counter.tomcatExpectedWebappSpecsApplied,
                counter.tomcatExpectedWebappItemsApplied,
                counter.dockerExpectedServiceSpecsApplied,
                counter.dockerExpectedServiceItemsApplied
        );
        configAuditService.recordProvisioningApply(environment.getId(), request.plan(), summary, List.copyOf(objects));

        return new ProvisioningPlanApplyResultDto(
                environment.getId(),
                summary,
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

    private void applyTomcatExpectedWebapps(
            UUID environmentId,
            UUID serverId,
            ProvisioningServerDto server,
            List<ProvisioningAppliedObjectDto> objects,
            Counter counter
    ) {
        if (server.tomcatExpectedWebapps().changeMode() != ProvisioningExpectedSetChangeModeDto.REPLACE) {
            return;
        }

        List<TomcatExpectedWebappsSpecDto> specs = server.tomcatExpectedWebapps().specs().stream()
                .map(spec -> new TomcatExpectedWebappsSpecDto(
                        serverId,
                        spec.role(),
                        spec.mode(),
                        spec.templateId(),
                        spec.items() == null ? List.of() : List.copyOf(spec.items())
                ))
                .toList();
        tomcatExpectedWebappsSpecService.replaceForServer(
                environmentId,
                serverId,
                new TomcatExpectedWebappsSpecReplaceRequestDto(specs)
        );
        counter.tomcatExpectedWebappSpecsApplied += specs.size();
        counter.tomcatExpectedWebappItemsApplied += countExplicitItems(server.tomcatExpectedWebapps().specs());
        objects.add(new ProvisioningAppliedObjectDto(
                ProvisioningPlanObjectTypeDto.TOMCAT_EXPECTED_WEBAPPS,
                server.clientRef().trim(),
                serverId,
                "replace tomcat expected webapps (" + specs.size() + " specs)"
        ));
    }

    private void applyDockerExpectedServices(
            UUID environmentId,
            UUID serverId,
            ProvisioningServerDto server,
            List<ProvisioningAppliedObjectDto> objects,
            Counter counter
    ) {
        if (server.dockerExpectedServices().changeMode() != ProvisioningExpectedSetChangeModeDto.REPLACE) {
            return;
        }

        List<DockerExpectedServicesSpecDto> specs = server.dockerExpectedServices().specs().stream()
                .map(spec -> new DockerExpectedServicesSpecDto(
                        serverId,
                        spec.mode(),
                        spec.templateId(),
                        spec.items() == null ? List.of() : List.copyOf(spec.items())
                ))
                .toList();
        dockerExpectedServicesSpecService.replaceForServer(
                environmentId,
                serverId,
                new DockerExpectedServicesSpecReplaceRequestDto(specs)
        );
        counter.dockerExpectedServiceSpecsApplied += specs.size();
        counter.dockerExpectedServiceItemsApplied += countExplicitDockerItems(server.dockerExpectedServices().specs());
        objects.add(new ProvisioningAppliedObjectDto(
                ProvisioningPlanObjectTypeDto.DOCKER_EXPECTED_SERVICES,
                server.clientRef().trim(),
                serverId,
                "replace docker expected services (" + specs.size() + " specs)"
        ));
    }

    private static int countExplicitItems(List<ProvisioningTomcatExpectedWebappsSpecDto> specs) {
        int count = 0;
        for (ProvisioningTomcatExpectedWebappsSpecDto spec : specs) {
            if (spec.mode() == ExpectedSetMode.EXPLICIT) {
                count += spec.items() == null ? 0 : spec.items().size();
            }
        }
        return count;
    }

    private static int countExplicitDockerItems(List<ProvisioningDockerExpectedServicesSpecDto> specs) {
        int count = 0;
        for (ProvisioningDockerExpectedServicesSpecDto spec : specs) {
            if (spec.mode() == ExpectedSetMode.EXPLICIT) {
                count += spec.items() == null ? 0 : spec.items().size();
            }
        }
        return count;
    }

    private static final class Counter {
        int environmentsCreated;
        int serversCreated;
        int tomcatTargetsCreated;
        int actuatorTargetsCreated;
        int tomcatExpectedWebappSpecsApplied;
        int tomcatExpectedWebappItemsApplied;
        int dockerExpectedServiceSpecsApplied;
        int dockerExpectedServiceItemsApplied;
    }
}
