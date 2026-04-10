package io.pockethive.hivewatch.service.provisioning;

import io.pockethive.hivewatch.service.actuator.ActuatorTargetEntity;
import io.pockethive.hivewatch.service.actuator.ActuatorTargetRepository;
import io.pockethive.hivewatch.service.api.EnvironmentProvisioningPlanDto;
import io.pockethive.hivewatch.service.api.ExpectedSetMode;
import io.pockethive.hivewatch.service.api.ProvisioningActuatorTargetDto;
import io.pockethive.hivewatch.service.api.ProvisioningChangeModeDto;
import io.pockethive.hivewatch.service.api.ProvisioningDockerExpectedServicesDto;
import io.pockethive.hivewatch.service.api.ProvisioningEnvironmentDto;
import io.pockethive.hivewatch.service.api.ProvisioningExpectedSetChangeModeDto;
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
import io.pockethive.hivewatch.service.expectedsets.ExpectedSetTemplateRepository;
import io.pockethive.hivewatch.service.tomcat.TomcatTargetEntity;
import io.pockethive.hivewatch.service.tomcat.TomcatTargetRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProvisioningPlanValidationServiceTest {
    private final EnvironmentRepository environmentRepository = mock(EnvironmentRepository.class);
    private final ServerRepository serverRepository = mock(ServerRepository.class);
    private final TomcatTargetRepository tomcatTargetRepository = mock(TomcatTargetRepository.class);
    private final ActuatorTargetRepository actuatorTargetRepository = mock(ActuatorTargetRepository.class);
    private final ExpectedSetTemplateRepository expectedSetTemplateRepository = mock(ExpectedSetTemplateRepository.class);

    private final ProvisioningPlanValidationService service = new ProvisioningPlanValidationService(
            environmentRepository,
            serverRepository,
            tomcatTargetRepository,
            actuatorTargetRepository,
            expectedSetTemplateRepository
    );

    @Test
    void validatesCreateEnvironmentPlan() {
        when(environmentRepository.findByName("NFT-03")).thenReturn(Optional.empty());

        ProvisioningPlanValidationResultDto result = service.validate(createPlan());

        assertThat(result.valid()).isTrue();
        assertThat(result.errors()).isEmpty();
        assertThat(result.diff()).extracting("objectType")
                .contains(
                        ProvisioningPlanObjectTypeDto.ENVIRONMENT,
                        ProvisioningPlanObjectTypeDto.SERVER,
                        ProvisioningPlanObjectTypeDto.TOMCAT_TARGET,
                        ProvisioningPlanObjectTypeDto.ACTUATOR_TARGET
                );
    }

    @Test
    void rejectsDuplicateEnvironmentNameAndInvalidTargetFields() {
        when(environmentRepository.findByName("NFT-03")).thenReturn(Optional.of(new EnvironmentEntity(UUID.randomUUID(), "NFT-03")));

        EnvironmentProvisioningPlanDto plan = new EnvironmentProvisioningPlanDto(
                "MCP",
                "corr-1",
                "invalid plan",
                new ProvisioningEnvironmentDto(ProvisioningChangeModeDto.CREATE, null, "NFT-03"),
                List.of(new ProvisioningServerDto(
                        "touchpoint",
                        ProvisioningChangeModeDto.CREATE,
                        null,
                        "Touchpoint",
                        List.of(new ProvisioningTomcatTargetDto(
                                TomcatRole.PAYMENTS,
                                TargetAdapterTypeDto.TOMCAT_MANAGER_HTML,
                                "http://nft03-tomcats.internal/manager/html",
                                8081,
                                "hc-manager",
                                "hc-manager-pass",
                                1500,
                                5000
                        )),
                        List.of(),
                        noTomcatExpectedChanges(),
                        noDockerExpectedChanges()
                ))
        );

        ProvisioningPlanValidationResultDto result = service.validate(plan);

        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).extracting("message")
                .contains("environment.name already exists", "baseUrl must not include a path");
    }

    @Test
    void rejectsDuplicateRolesWithinPlan() {
        when(environmentRepository.findByName("NFT-03")).thenReturn(Optional.empty());

        EnvironmentProvisioningPlanDto plan = new EnvironmentProvisioningPlanDto(
                "UI",
                null,
                null,
                new ProvisioningEnvironmentDto(ProvisioningChangeModeDto.CREATE, null, "NFT-03"),
                List.of(new ProvisioningServerDto(
                        "touchpoint",
                        ProvisioningChangeModeDto.CREATE,
                        null,
                        "Touchpoint",
                        List.of(
                                tomcat(TomcatRole.PAYMENTS, 8081),
                                tomcat(TomcatRole.PAYMENTS, 8082)
                        ),
                        List.of(),
                        noTomcatExpectedChanges(),
                        noDockerExpectedChanges()
                ))
        );

        ProvisioningPlanValidationResultDto result = service.validate(plan);

        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).anySatisfy(issue -> {
            assertThat(issue.severity()).isEqualTo(ProvisioningPlanIssueSeverityDto.ERROR);
            assertThat(issue.path()).contains("/role");
            assertThat(issue.message()).isEqualTo("role must be unique for this adapter within the server");
        });
    }

    @Test
    void rejectsRoleConflictOnExistingServer() {
        UUID environmentId = UUID.randomUUID();
        UUID serverId = UUID.randomUUID();
        when(environmentRepository.findById(environmentId)).thenReturn(Optional.of(new EnvironmentEntity(environmentId, "NFT-01")));
        when(serverRepository.findById(serverId)).thenReturn(Optional.of(new ServerEntity(serverId, environmentId, "Touchpoint", Instant.now())));
        when(tomcatTargetRepository.findByServerIdIn(List.of(serverId))).thenReturn(List.of(new TomcatTargetEntity(
                UUID.randomUUID(),
                serverId,
                TomcatRole.PAYMENTS,
                "http://nft01-tomcats.internal",
                8081,
                "hc-manager",
                "hc-manager-pass",
                1500,
                5000,
                Instant.now()
        )));
        when(actuatorTargetRepository.findByServerIdIn(List.of(serverId))).thenReturn(List.of());

        EnvironmentProvisioningPlanDto plan = new EnvironmentProvisioningPlanDto(
                "MCP",
                "corr-2",
                "extend existing env",
                new ProvisioningEnvironmentDto(ProvisioningChangeModeDto.EXISTING, environmentId, "NFT-01"),
                List.of(new ProvisioningServerDto(
                        "touchpoint",
                        ProvisioningChangeModeDto.EXISTING,
                        serverId,
                        "Touchpoint",
                        List.of(tomcat(TomcatRole.PAYMENTS, 8084)),
                        List.of(),
                        noTomcatExpectedChanges(),
                        noDockerExpectedChanges()
                ))
        );

        ProvisioningPlanValidationResultDto result = service.validate(plan);

        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).extracting("message")
                .contains("role already exists for this adapter on the server");
    }

    @Test
    void validatesTomcatExpectedWebappsReplaceAgainstConfiguredRoles() {
        when(environmentRepository.findByName("NFT-03")).thenReturn(Optional.empty());

        EnvironmentProvisioningPlanDto plan = new EnvironmentProvisioningPlanDto(
                "MCP",
                "corr-1",
                "Add NFT-03",
                new ProvisioningEnvironmentDto(ProvisioningChangeModeDto.CREATE, null, "NFT-03"),
                List.of(new ProvisioningServerDto(
                        "touchpoint",
                        ProvisioningChangeModeDto.CREATE,
                        null,
                        "Touchpoint",
                        List.of(tomcat(TomcatRole.PAYMENTS, 8081)),
                        List.of(),
                        new ProvisioningTomcatExpectedWebappsDto(
                                ProvisioningExpectedSetChangeModeDto.REPLACE,
                                List.of(new ProvisioningTomcatExpectedWebappsSpecDto(
                                        TomcatRole.PAYMENTS,
                                        ExpectedSetMode.EXPLICIT,
                                        null,
                                        List.of("/payments")
                                ))
                        ),
                        noDockerExpectedChanges()
                ))
        );

        ProvisioningPlanValidationResultDto result = service.validate(plan);

        assertThat(result.valid()).isTrue();
        assertThat(result.diff()).extracting("objectType")
                .contains(ProvisioningPlanObjectTypeDto.TOMCAT_EXPECTED_WEBAPPS);
    }

    @Test
    void rejectsInvalidTomcatExpectedWebappsSpec() {
        when(environmentRepository.findByName("NFT-03")).thenReturn(Optional.empty());

        EnvironmentProvisioningPlanDto plan = new EnvironmentProvisioningPlanDto(
                "MCP",
                "corr-1",
                "invalid expected",
                new ProvisioningEnvironmentDto(ProvisioningChangeModeDto.CREATE, null, "NFT-03"),
                List.of(new ProvisioningServerDto(
                        "touchpoint",
                        ProvisioningChangeModeDto.CREATE,
                        null,
                        "Touchpoint",
                        List.of(tomcat(TomcatRole.PAYMENTS, 8081)),
                        List.of(),
                        new ProvisioningTomcatExpectedWebappsDto(
                                ProvisioningExpectedSetChangeModeDto.REPLACE,
                                List.of(new ProvisioningTomcatExpectedWebappsSpecDto(
                                        TomcatRole.AUTH,
                                        ExpectedSetMode.EXPLICIT,
                                        null,
                                        List.of("payments", "/manager")
                                ))
                        ),
                        noDockerExpectedChanges()
                ))
        );

        ProvisioningPlanValidationResultDto result = service.validate(plan);

        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).extracting("message")
                .contains(
                        "role must match a configured Tomcat target on this server",
                        "Webapp path must start with '/': payments",
                        "Built-in Tomcat webapp is not allowed in expected list: /manager"
                );
    }

    private static EnvironmentProvisioningPlanDto createPlan() {
        return new EnvironmentProvisioningPlanDto(
                "MCP",
                "corr-1",
                "Add NFT-03",
                new ProvisioningEnvironmentDto(ProvisioningChangeModeDto.CREATE, null, "NFT-03"),
                List.of(new ProvisioningServerDto(
                        "touchpoint",
                        ProvisioningChangeModeDto.CREATE,
                        null,
                        "Touchpoint",
                        List.of(tomcat(TomcatRole.PAYMENTS, 8081)),
                        List.of(new ProvisioningActuatorTargetDto(
                                TomcatRole.PAYMENTS,
                                TargetAdapterTypeDto.ACTUATOR_HTTP,
                                "http://nft03-services.internal",
                                8080,
                                "payments",
                                1500,
                                5000
                        )),
                        noTomcatExpectedChanges(),
                        noDockerExpectedChanges()
                ))
        );
    }

    private static ProvisioningTomcatExpectedWebappsDto noTomcatExpectedChanges() {
        return new ProvisioningTomcatExpectedWebappsDto(
                ProvisioningExpectedSetChangeModeDto.NO_CHANGE,
                List.of()
        );
    }

    private static ProvisioningDockerExpectedServicesDto noDockerExpectedChanges() {
        return new ProvisioningDockerExpectedServicesDto(
                ProvisioningExpectedSetChangeModeDto.NO_CHANGE,
                List.of()
        );
    }

    private static ProvisioningTomcatTargetDto tomcat(TomcatRole role, int port) {
        return new ProvisioningTomcatTargetDto(
                role,
                TargetAdapterTypeDto.TOMCAT_MANAGER_HTML,
                "http://nft03-tomcats.internal",
                port,
                "hc-manager",
                "hc-manager-pass",
                1500,
                5000
        );
    }
}
