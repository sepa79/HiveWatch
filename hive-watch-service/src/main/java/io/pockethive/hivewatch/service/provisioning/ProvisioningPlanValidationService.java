package io.pockethive.hivewatch.service.provisioning;

import io.pockethive.hivewatch.service.actuator.ActuatorTargetEntity;
import io.pockethive.hivewatch.service.actuator.ActuatorTargetRepository;
import io.pockethive.hivewatch.service.actuator.ActuatorTargetValidation;
import io.pockethive.hivewatch.service.api.EnvironmentProvisioningPlanDto;
import io.pockethive.hivewatch.service.api.ExpectedSetMode;
import io.pockethive.hivewatch.service.api.ExpectedSetTemplateKind;
import io.pockethive.hivewatch.service.api.ProvisioningActuatorTargetDto;
import io.pockethive.hivewatch.service.api.ProvisioningChangeModeDto;
import io.pockethive.hivewatch.service.api.ProvisioningDockerExpectedServicesDto;
import io.pockethive.hivewatch.service.api.ProvisioningDockerExpectedServicesSpecDto;
import io.pockethive.hivewatch.service.api.ProvisioningEnvironmentDto;
import io.pockethive.hivewatch.service.api.ProvisioningExpectedSetChangeModeDto;
import io.pockethive.hivewatch.service.api.ProvisioningPlanDiffActionDto;
import io.pockethive.hivewatch.service.api.ProvisioningPlanDiffDto;
import io.pockethive.hivewatch.service.api.ProvisioningPlanIssueDto;
import io.pockethive.hivewatch.service.api.ProvisioningPlanIssueSeverityDto;
import io.pockethive.hivewatch.service.api.ProvisioningPlanObjectTypeDto;
import io.pockethive.hivewatch.service.api.ProvisioningPlanValidationResultDto;
import io.pockethive.hivewatch.service.api.ProvisioningServerDto;
import io.pockethive.hivewatch.service.api.ProvisioningTomcatExpectedWebappsDto;
import io.pockethive.hivewatch.service.api.ProvisioningTomcatExpectedWebappsSpecDto;
import io.pockethive.hivewatch.service.api.ProvisioningTomcatTargetDto;
import io.pockethive.hivewatch.service.api.TargetAdapterTypeDto;
import io.pockethive.hivewatch.service.api.TomcatRole;
import io.pockethive.hivewatch.service.environments.EnvironmentEntity;
import io.pockethive.hivewatch.service.environments.EnvironmentRepository;
import io.pockethive.hivewatch.service.environments.servers.ServerEntity;
import io.pockethive.hivewatch.service.environments.servers.ServerRepository;
import io.pockethive.hivewatch.service.expectedsets.ExpectedSetItemValidationIssue;
import io.pockethive.hivewatch.service.expectedsets.ExpectedSetSpecValidation;
import io.pockethive.hivewatch.service.expectedsets.ExpectedSetTemplateEntity;
import io.pockethive.hivewatch.service.expectedsets.ExpectedSetTemplateRepository;
import io.pockethive.hivewatch.service.targets.TargetConnectionValidation;
import io.pockethive.hivewatch.service.tomcat.TomcatTargetEntity;
import io.pockethive.hivewatch.service.tomcat.TomcatTargetRepository;
import io.pockethive.hivewatch.service.tomcat.TomcatTargetValidation;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Service
public class ProvisioningPlanValidationService {
    private final EnvironmentRepository environmentRepository;
    private final ServerRepository serverRepository;
    private final TomcatTargetRepository tomcatTargetRepository;
    private final ActuatorTargetRepository actuatorTargetRepository;
    private final ExpectedSetTemplateRepository expectedSetTemplateRepository;

    public ProvisioningPlanValidationService(
            EnvironmentRepository environmentRepository,
            ServerRepository serverRepository,
            TomcatTargetRepository tomcatTargetRepository,
            ActuatorTargetRepository actuatorTargetRepository,
            ExpectedSetTemplateRepository expectedSetTemplateRepository
    ) {
        this.environmentRepository = environmentRepository;
        this.serverRepository = serverRepository;
        this.tomcatTargetRepository = tomcatTargetRepository;
        this.actuatorTargetRepository = actuatorTargetRepository;
        this.expectedSetTemplateRepository = expectedSetTemplateRepository;
    }

    @Transactional(readOnly = true)
    public ProvisioningPlanValidationResultDto validate(EnvironmentProvisioningPlanDto plan) {
        if (plan == null) {
            throw new ResponseStatusException(BAD_REQUEST, "Request body is required");
        }

        List<ProvisioningPlanIssueDto> errors = new ArrayList<>();
        List<ProvisioningPlanIssueDto> warnings = new ArrayList<>();
        List<ProvisioningPlanDiffDto> diff = new ArrayList<>();

        if (trimmed(plan.source()) == null) {
            error(errors, "/source", "source is required");
        }
        Optional<EnvironmentEntity> existingEnvironment = validateEnvironment(plan.environment(), errors, diff);
        validateServers(plan, existingEnvironment.map(EnvironmentEntity::getId).orElse(null), errors, warnings, diff);

        return new ProvisioningPlanValidationResultDto(
                errors.isEmpty(),
                List.copyOf(errors),
                List.copyOf(warnings),
                List.copyOf(diff)
        );
    }

    private Optional<EnvironmentEntity> validateEnvironment(
            ProvisioningEnvironmentDto environment,
            List<ProvisioningPlanIssueDto> errors,
            List<ProvisioningPlanDiffDto> diff
    ) {
        if (environment == null) {
            error(errors, "/environment", "environment is required");
            return Optional.empty();
        }
        if (environment.mode() == null) {
            error(errors, "/environment/mode", "environment.mode is required");
            return Optional.empty();
        }

        if (environment.mode() == ProvisioningChangeModeDto.CREATE) {
            String name = trimmed(environment.name());
            if (name == null) {
                error(errors, "/environment/name", "environment.name is required");
                return Optional.empty();
            }
            if (environment.environmentId() != null) {
                error(errors, "/environment/environmentId", "environment.environmentId must be null for CREATE");
            }
            if (environmentRepository.findByName(name).isPresent()) {
                error(errors, "/environment/name", "environment.name already exists");
            }
            diff.add(new ProvisioningPlanDiffDto(
                    ProvisioningPlanDiffActionDto.CREATE,
                    ProvisioningPlanObjectTypeDto.ENVIRONMENT,
                    null,
                    name
            ));
            return Optional.empty();
        }

        if (environment.environmentId() == null) {
            error(errors, "/environment/environmentId", "environment.environmentId is required for EXISTING");
            return Optional.empty();
        }
        Optional<EnvironmentEntity> existing = environmentRepository.findById(environment.environmentId());
        if (existing.isEmpty()) {
            error(errors, "/environment/environmentId", "environment not found");
            return Optional.empty();
        }
        String name = trimmed(environment.name());
        if (name == null) {
            error(errors, "/environment/name", "environment.name is required");
        } else if (!existing.get().getName().equals(name)) {
            error(errors, "/environment/name", "environment.name must match the existing environment");
        }
        diff.add(new ProvisioningPlanDiffDto(
                ProvisioningPlanDiffActionDto.REFERENCE,
                ProvisioningPlanObjectTypeDto.ENVIRONMENT,
                existing.get().getId().toString(),
                existing.get().getName()
        ));
        return existing;
    }

    private void validateServers(
            EnvironmentProvisioningPlanDto plan,
            UUID existingEnvironmentId,
            List<ProvisioningPlanIssueDto> errors,
            List<ProvisioningPlanIssueDto> warnings,
            List<ProvisioningPlanDiffDto> diff
    ) {
        List<ProvisioningServerDto> servers = plan.servers();
        if (servers == null) {
            error(errors, "/servers", "servers is required");
            return;
        }
        if (servers.isEmpty()) {
            error(errors, "/servers", "servers must contain at least one server");
            return;
        }

        Set<String> clientRefs = new HashSet<>();
        Set<String> createNames = new HashSet<>();
        for (int i = 0; i < servers.size(); i++) {
            validateServer(servers.get(i), i, existingEnvironmentId, clientRefs, createNames, errors, warnings, diff);
        }
    }

    private void validateServer(
            ProvisioningServerDto server,
            int index,
            UUID existingEnvironmentId,
            Set<String> clientRefs,
            Set<String> createNames,
            List<ProvisioningPlanIssueDto> errors,
            List<ProvisioningPlanIssueDto> warnings,
            List<ProvisioningPlanDiffDto> diff
    ) {
        String path = "/servers/" + index;
        if (server == null) {
            error(errors, path, "server is required");
            return;
        }
        if (server.mode() == null) {
            error(errors, path + "/mode", "server.mode is required");
            return;
        }
        String clientRef = trimmed(server.clientRef());
        if (clientRef == null) {
            error(errors, path + "/clientRef", "server.clientRef is required");
        } else if (!clientRefs.add(clientRef)) {
            error(errors, path + "/clientRef", "server.clientRef must be unique within the plan");
        }

        UUID serverId = null;
        if (server.mode() == ProvisioningChangeModeDto.CREATE) {
            String name = trimmed(server.name());
            if (name == null) {
                error(errors, path + "/name", "server.name is required");
            } else {
                String key = name.toLowerCase();
                if (!createNames.add(key)) {
                    error(errors, path + "/name", "server.name must be unique among CREATE servers");
                }
                if (existingEnvironmentId != null && serverRepository.existsByEnvironmentIdAndName(existingEnvironmentId, name)) {
                    error(errors, path + "/name", "server.name already exists in environment");
                }
                diff.add(new ProvisioningPlanDiffDto(
                        ProvisioningPlanDiffActionDto.CREATE,
                        ProvisioningPlanObjectTypeDto.SERVER,
                        clientRef,
                        name
                ));
            }
            if (server.serverId() != null) {
                error(errors, path + "/serverId", "server.serverId must be null for CREATE");
            }
        } else {
            serverId = validateExistingServer(server, path, existingEnvironmentId, errors, diff);
        }

        TargetRoleState roleState = validateTargets(server, path, serverId, errors, warnings, diff);
        validateExpectedSets(server, path, trimmed(server.clientRef()), roleState, errors, diff);
    }

    private UUID validateExistingServer(
            ProvisioningServerDto server,
            String path,
            UUID existingEnvironmentId,
            List<ProvisioningPlanIssueDto> errors,
            List<ProvisioningPlanDiffDto> diff
    ) {
        if (existingEnvironmentId == null) {
            error(errors, path + "/mode", "server.mode EXISTING requires environment.mode EXISTING");
            return null;
        }
        if (server.serverId() == null) {
            error(errors, path + "/serverId", "server.serverId is required for EXISTING");
            return null;
        }
        Optional<ServerEntity> existing = serverRepository.findById(server.serverId());
        if (existing.isEmpty() || !existing.get().getEnvironmentId().equals(existingEnvironmentId)) {
            error(errors, path + "/serverId", "server not found in environment");
            return null;
        }
        String name = trimmed(server.name());
        if (name == null) {
            error(errors, path + "/name", "server.name is required");
        } else if (!existing.get().getName().equals(name)) {
            error(errors, path + "/name", "server.name must match the existing server");
        }
        diff.add(new ProvisioningPlanDiffDto(
                ProvisioningPlanDiffActionDto.REFERENCE,
                ProvisioningPlanObjectTypeDto.SERVER,
                trimmed(server.clientRef()),
                existing.get().getName()
        ));
        return existing.get().getId();
    }

    private TargetRoleState validateTargets(
            ProvisioningServerDto server,
            String path,
            UUID existingServerId,
            List<ProvisioningPlanIssueDto> errors,
            List<ProvisioningPlanIssueDto> warnings,
            List<ProvisioningPlanDiffDto> diff
    ) {
        if (server.tomcatTargets() == null) {
            error(errors, path + "/tomcatTargets", "server.tomcatTargets is required");
            return TargetRoleState.empty();
        }
        if (server.actuatorTargets() == null) {
            error(errors, path + "/actuatorTargets", "server.actuatorTargets is required");
            return TargetRoleState.empty();
        }
        if (server.tomcatTargets().isEmpty() && server.actuatorTargets().isEmpty()) {
            warning(warnings, path, "server has no targets");
        }

        Set<TomcatRole> existingTomcatRoles = existingServerId == null
                ? EnumSet.noneOf(TomcatRole.class)
                : existingTomcatRoles(existingServerId);
        Set<TomcatRole> existingActuatorRoles = existingServerId == null
                ? EnumSet.noneOf(TomcatRole.class)
                : existingActuatorRoles(existingServerId);

        Set<TomcatRole> tomcatRoles = EnumSet.noneOf(TomcatRole.class);
        for (int i = 0; i < server.tomcatTargets().size(); i++) {
            validateTomcatTarget(server.tomcatTargets().get(i), path + "/tomcatTargets/" + i, trimmed(server.clientRef()), tomcatRoles, existingTomcatRoles, errors, diff);
        }

        Set<TomcatRole> actuatorRoles = EnumSet.noneOf(TomcatRole.class);
        for (int i = 0; i < server.actuatorTargets().size(); i++) {
            validateActuatorTarget(server.actuatorTargets().get(i), path + "/actuatorTargets/" + i, trimmed(server.clientRef()), actuatorRoles, existingActuatorRoles, errors, diff);
        }

        Set<TomcatRole> availableTomcatRoles = EnumSet.noneOf(TomcatRole.class);
        availableTomcatRoles.addAll(existingTomcatRoles);
        availableTomcatRoles.addAll(tomcatRoles);
        return new TargetRoleState(availableTomcatRoles);
    }

    private void validateExpectedSets(
            ProvisioningServerDto server,
            String path,
            String serverClientRef,
            TargetRoleState roleState,
            List<ProvisioningPlanIssueDto> errors,
            List<ProvisioningPlanDiffDto> diff
    ) {
        validateTomcatExpectedWebapps(server.tomcatExpectedWebapps(), path + "/tomcatExpectedWebapps", serverClientRef, roleState.tomcatRoles(), errors, diff);
        validateDockerExpectedServices(server.dockerExpectedServices(), path + "/dockerExpectedServices", serverClientRef, errors, diff);
    }

    private void validateTomcatExpectedWebapps(
            ProvisioningTomcatExpectedWebappsDto expected,
            String path,
            String serverClientRef,
            Set<TomcatRole> availableRoles,
            List<ProvisioningPlanIssueDto> errors,
            List<ProvisioningPlanDiffDto> diff
    ) {
        if (expected == null) {
            error(errors, path, "server.tomcatExpectedWebapps is required");
            return;
        }
        if (expected.changeMode() == null) {
            error(errors, path + "/changeMode", "changeMode is required");
            return;
        }
        if (expected.specs() == null) {
            error(errors, path + "/specs", "specs is required");
            return;
        }
        if (expected.changeMode() == ProvisioningExpectedSetChangeModeDto.NO_CHANGE) {
            if (!expected.specs().isEmpty()) {
                error(errors, path + "/specs", "specs must be empty when changeMode is NO_CHANGE");
            }
            return;
        }

        Set<TomcatRole> seenRoles = EnumSet.noneOf(TomcatRole.class);
        for (int i = 0; i < expected.specs().size(); i++) {
            validateTomcatExpectedWebappsSpec(expected.specs().get(i), path + "/specs/" + i, availableRoles, seenRoles, errors);
        }
        diff.add(new ProvisioningPlanDiffDto(
                ProvisioningPlanDiffActionDto.REPLACE,
                ProvisioningPlanObjectTypeDto.TOMCAT_EXPECTED_WEBAPPS,
                serverClientRef,
                "replace tomcat expected webapps (" + expected.specs().size() + " specs)"
        ));
    }

    private void validateTomcatExpectedWebappsSpec(
            ProvisioningTomcatExpectedWebappsSpecDto spec,
            String path,
            Set<TomcatRole> availableRoles,
            Set<TomcatRole> seenRoles,
            List<ProvisioningPlanIssueDto> errors
    ) {
        if (spec == null) {
            error(errors, path, "spec is required");
            return;
        }
        if (spec.role() == null) {
            error(errors, path + "/role", "role is required");
        } else {
            if (!seenRoles.add(spec.role())) {
                error(errors, path + "/role", "role must be unique within tomcatExpectedWebapps");
            }
            if (!availableRoles.contains(spec.role())) {
                error(errors, path + "/role", "role must match a configured Tomcat target on this server");
            }
        }
        validateExpectedSpecMode(spec.mode(), spec.templateId(), spec.items(), path, ExpectedSetTemplateKind.TOMCAT_WEBAPP_PATH, errors);
        if (spec.mode() == ExpectedSetMode.EXPLICIT) {
            addItemValidationIssues(
                    ExpectedSetSpecValidation.validateTomcatWebappPaths(spec.items() == null ? List.of() : spec.items()),
                    path + "/items",
                    errors
            );
        }
    }

    private void validateDockerExpectedServices(
            ProvisioningDockerExpectedServicesDto expected,
            String path,
            String serverClientRef,
            List<ProvisioningPlanIssueDto> errors,
            List<ProvisioningPlanDiffDto> diff
    ) {
        if (expected == null) {
            error(errors, path, "server.dockerExpectedServices is required");
            return;
        }
        if (expected.changeMode() == null) {
            error(errors, path + "/changeMode", "changeMode is required");
            return;
        }
        if (expected.specs() == null) {
            error(errors, path + "/specs", "specs is required");
            return;
        }
        if (expected.changeMode() == ProvisioningExpectedSetChangeModeDto.NO_CHANGE) {
            if (!expected.specs().isEmpty()) {
                error(errors, path + "/specs", "specs must be empty when changeMode is NO_CHANGE");
            }
            return;
        }
        if (expected.specs().size() > 1) {
            error(errors, path + "/specs", "dockerExpectedServices specs must contain at most one item");
            return;
        }
        for (int i = 0; i < expected.specs().size(); i++) {
            validateDockerExpectedServicesSpec(expected.specs().get(i), path + "/specs/" + i, errors);
        }
        diff.add(new ProvisioningPlanDiffDto(
                ProvisioningPlanDiffActionDto.REPLACE,
                ProvisioningPlanObjectTypeDto.DOCKER_EXPECTED_SERVICES,
                serverClientRef,
                "replace docker expected services (" + expected.specs().size() + " specs)"
        ));
    }

    private void validateDockerExpectedServicesSpec(
            ProvisioningDockerExpectedServicesSpecDto spec,
            String path,
            List<ProvisioningPlanIssueDto> errors
    ) {
        if (spec == null) {
            error(errors, path, "spec is required");
            return;
        }
        validateExpectedSpecMode(spec.mode(), spec.templateId(), spec.items(), path, ExpectedSetTemplateKind.DOCKER_SERVICE_PROFILE, errors);
        if (spec.mode() == ExpectedSetMode.EXPLICIT) {
            addItemValidationIssues(
                    ExpectedSetSpecValidation.validateDockerProfiles(spec.items() == null ? List.of() : spec.items()),
                    path + "/items",
                    errors
            );
        }
    }

    private void validateExpectedSpecMode(
            ExpectedSetMode mode,
            UUID templateId,
            List<String> items,
            String path,
            ExpectedSetTemplateKind expectedTemplateKind,
            List<ProvisioningPlanIssueDto> errors
    ) {
        if (mode == null) {
            error(errors, path + "/mode", "mode is required");
            return;
        }
        if (mode == ExpectedSetMode.UNCONFIGURED) {
            error(errors, path + "/mode", "mode cannot be UNCONFIGURED; use REPLACE with an empty specs list to clear expected sets");
            return;
        }
        if (mode == ExpectedSetMode.TEMPLATE) {
            if (templateId == null) {
                error(errors, path + "/templateId", "templateId is required for TEMPLATE mode");
            } else {
                validateTemplate(templateId, path + "/templateId", expectedTemplateKind, errors);
            }
            if (items != null && !items.isEmpty()) {
                error(errors, path + "/items", "items must be empty in TEMPLATE mode");
            }
            return;
        }
        if (templateId != null) {
            error(errors, path + "/templateId", "templateId must be null in EXPLICIT mode");
        }
    }

    private void validateTemplate(UUID templateId, String path, ExpectedSetTemplateKind expectedTemplateKind, List<ProvisioningPlanIssueDto> errors) {
        Optional<ExpectedSetTemplateEntity> template = expectedSetTemplateRepository.findById(templateId);
        if (template.isEmpty()) {
            error(errors, path, "template not found");
        } else if (template.get().getKind() != expectedTemplateKind) {
            error(errors, path, "template kind must be " + expectedTemplateKind);
        }
    }

    private void addItemValidationIssues(
            List<ExpectedSetItemValidationIssue> itemIssues,
            String path,
            List<ProvisioningPlanIssueDto> errors
    ) {
        for (ExpectedSetItemValidationIssue issue : itemIssues) {
            error(errors, path + "/" + issue.index(), issue.message());
        }
    }

    private void validateTomcatTarget(
            ProvisioningTomcatTargetDto target,
            String path,
            String serverClientRef,
            Set<TomcatRole> planRoles,
            Set<TomcatRole> existingRoles,
            List<ProvisioningPlanIssueDto> errors,
            List<ProvisioningPlanDiffDto> diff
    ) {
        if (target == null) {
            error(errors, path, "tomcat target is required");
            return;
        }
        if (target.adapterType() == null) {
            error(errors, path + "/adapterType", "adapterType is required");
        } else if (target.adapterType() != TargetAdapterTypeDto.TOMCAT_MANAGER_HTML) {
            error(errors, path + "/adapterType", "adapterType must be TOMCAT_MANAGER_HTML");
        }
        validateRole(target.role(), path, planRoles, existingRoles, errors);
        validateTomcatFields(target, path, errors);
        if (target.role() != null) {
            diff.add(new ProvisioningPlanDiffDto(
                    ProvisioningPlanDiffActionDto.CREATE,
                    ProvisioningPlanObjectTypeDto.TOMCAT_TARGET,
                    serverClientRef,
                    "TOMCAT_MANAGER_HTML " + target.role() + " " + safeTargetLabel(target.baseUrl(), target.port())
            ));
        }
    }

    private void validateActuatorTarget(
            ProvisioningActuatorTargetDto target,
            String path,
            String serverClientRef,
            Set<TomcatRole> planRoles,
            Set<TomcatRole> existingRoles,
            List<ProvisioningPlanIssueDto> errors,
            List<ProvisioningPlanDiffDto> diff
    ) {
        if (target == null) {
            error(errors, path, "actuator target is required");
            return;
        }
        if (target.adapterType() == null) {
            error(errors, path + "/adapterType", "adapterType is required");
        } else if (target.adapterType() != TargetAdapterTypeDto.ACTUATOR_HTTP) {
            error(errors, path + "/adapterType", "adapterType must be ACTUATOR_HTTP");
        }
        validateRole(target.role(), path, planRoles, existingRoles, errors);
        validateActuatorFields(target, path, errors);
        if (target.role() != null) {
            diff.add(new ProvisioningPlanDiffDto(
                    ProvisioningPlanDiffActionDto.CREATE,
                    ProvisioningPlanObjectTypeDto.ACTUATOR_TARGET,
                    serverClientRef,
                    "ACTUATOR_HTTP " + target.role() + " " + safeTargetLabel(target.baseUrl(), target.port())
            ));
        }
    }

    private void validateRole(
            TomcatRole role,
            String path,
            Set<TomcatRole> planRoles,
            Set<TomcatRole> existingRoles,
            List<ProvisioningPlanIssueDto> errors
    ) {
        if (role == null) {
            error(errors, path + "/role", "role is required");
            return;
        }
        if (!planRoles.add(role)) {
            error(errors, path + "/role", "role must be unique for this adapter within the server");
        }
        if (existingRoles.contains(role)) {
            error(errors, path + "/role", "role already exists for this adapter on the server");
        }
    }

    private void validateTomcatFields(ProvisioningTomcatTargetDto target, String path, List<ProvisioningPlanIssueDto> errors) {
        tryValidate(errors, path + "/baseUrl", () -> TomcatTargetValidation.parseBaseUrl(target.baseUrl()));
        tryValidate(errors, path + "/port", () -> TargetConnectionValidation.validatePort(target.port()));
        tryValidate(errors, path + "/username", () -> TomcatTargetValidation.sanitizeUsername(target.username()));
        tryValidate(errors, path + "/password", () -> TomcatTargetValidation.requirePassword(target.password()));
        tryValidate(errors, path + "/timeouts", () -> TargetConnectionValidation.validateTimeouts(target.connectTimeoutMs(), target.requestTimeoutMs()));
    }

    private void validateActuatorFields(ProvisioningActuatorTargetDto target, String path, List<ProvisioningPlanIssueDto> errors) {
        tryValidate(errors, path + "/baseUrl", () -> ActuatorTargetValidation.parseBaseUrl(target.baseUrl()));
        tryValidate(errors, path + "/port", () -> TargetConnectionValidation.validatePort(target.port()));
        tryValidate(errors, path + "/profile", () -> ActuatorTargetValidation.sanitizeProfile(target.profile()));
        tryValidate(errors, path + "/timeouts", () -> TargetConnectionValidation.validateTimeouts(target.connectTimeoutMs(), target.requestTimeoutMs()));
    }

    private Set<TomcatRole> existingTomcatRoles(UUID serverId) {
        Set<TomcatRole> roles = EnumSet.noneOf(TomcatRole.class);
        for (TomcatTargetEntity target : tomcatTargetRepository.findByServerIdIn(List.of(serverId))) {
            roles.add(target.getRole());
        }
        return roles;
    }

    private Set<TomcatRole> existingActuatorRoles(UUID serverId) {
        Set<TomcatRole> roles = EnumSet.noneOf(TomcatRole.class);
        for (ActuatorTargetEntity target : actuatorTargetRepository.findByServerIdIn(List.of(serverId))) {
            roles.add(target.getRole());
        }
        return roles;
    }

    private static void tryValidate(List<ProvisioningPlanIssueDto> errors, String path, ValidationBlock block) {
        try {
            block.run();
        } catch (IllegalArgumentException e) {
            error(errors, path, e.getMessage());
        }
    }

    private static String safeTargetLabel(String baseUrl, int port) {
        String trimmed = trimmed(baseUrl);
        return (trimmed == null ? "<missing-baseUrl>" : trimmed) + ":" + port;
    }

    private static String trimmed(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    private static void error(List<ProvisioningPlanIssueDto> issues, String path, String message) {
        issues.add(new ProvisioningPlanIssueDto(ProvisioningPlanIssueSeverityDto.ERROR, path, message));
    }

    private static void warning(List<ProvisioningPlanIssueDto> issues, String path, String message) {
        issues.add(new ProvisioningPlanIssueDto(ProvisioningPlanIssueSeverityDto.WARNING, path, message));
    }

    private interface ValidationBlock {
        void run();
    }

    private record TargetRoleState(Set<TomcatRole> tomcatRoles) {
        private static TargetRoleState empty() {
            return new TargetRoleState(EnumSet.noneOf(TomcatRole.class));
        }
    }
}
