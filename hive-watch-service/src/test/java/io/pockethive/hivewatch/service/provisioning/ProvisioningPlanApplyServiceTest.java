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
import io.pockethive.hivewatch.service.api.ProvisioningPlanApplyRequestDto;
import io.pockethive.hivewatch.service.api.ProvisioningPlanIssueDto;
import io.pockethive.hivewatch.service.api.ProvisioningPlanIssueSeverityDto;
import io.pockethive.hivewatch.service.api.ProvisioningPlanObjectTypeDto;
import io.pockethive.hivewatch.service.api.ProvisioningPlanValidationResultDto;
import io.pockethive.hivewatch.service.api.ProvisioningServerDto;
import io.pockethive.hivewatch.service.api.ProvisioningTomcatExpectedWebappsDto;
import io.pockethive.hivewatch.service.api.ProvisioningTomcatExpectedWebappsSpecDto;
import io.pockethive.hivewatch.service.api.ProvisioningTomcatTargetDto;
import io.pockethive.hivewatch.service.api.TargetAdapterTypeDto;
import io.pockethive.hivewatch.service.api.TomcatExpectedWebappsSpecReplaceRequestDto;
import io.pockethive.hivewatch.service.api.TomcatRole;
import io.pockethive.hivewatch.service.environments.EnvironmentEntity;
import io.pockethive.hivewatch.service.environments.EnvironmentRepository;
import io.pockethive.hivewatch.service.environments.servers.ServerEntity;
import io.pockethive.hivewatch.service.environments.servers.ServerRepository;
import io.pockethive.hivewatch.service.expectedsets.DockerExpectedServicesSpecService;
import io.pockethive.hivewatch.service.expectedsets.TomcatExpectedWebappsSpecService;
import io.pockethive.hivewatch.service.tomcat.TomcatTargetEntity;
import io.pockethive.hivewatch.service.tomcat.TomcatTargetRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProvisioningPlanApplyServiceTest {
    private final ProvisioningPlanValidationService validationService = mock(ProvisioningPlanValidationService.class);
    private final EnvironmentRepository environmentRepository = mock(EnvironmentRepository.class);
    private final ServerRepository serverRepository = mock(ServerRepository.class);
    private final TomcatTargetRepository tomcatTargetRepository = mock(TomcatTargetRepository.class);
    private final ActuatorTargetRepository actuatorTargetRepository = mock(ActuatorTargetRepository.class);
    private final TomcatExpectedWebappsSpecService tomcatExpectedWebappsSpecService = mock(TomcatExpectedWebappsSpecService.class);
    private final DockerExpectedServicesSpecService dockerExpectedServicesSpecService = mock(DockerExpectedServicesSpecService.class);

    private final ProvisioningPlanApplyService service = new ProvisioningPlanApplyService(
            validationService,
            environmentRepository,
            serverRepository,
            tomcatTargetRepository,
            actuatorTargetRepository,
            tomcatExpectedWebappsSpecService,
            dockerExpectedServicesSpecService
    );

    @Test
    void appliesCreatePlanAfterValidation() {
        EnvironmentProvisioningPlanDto plan = createPlan();
        when(validationService.validate(plan)).thenReturn(valid());
        when(environmentRepository.save(any(EnvironmentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(serverRepository.save(any(ServerEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(tomcatTargetRepository.save(any(TomcatTargetEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(actuatorTargetRepository.save(any(ActuatorTargetEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.apply(new ProvisioningPlanApplyRequestDto(plan, false));

        assertThat(result.environmentId()).isNotNull();
        assertThat(result.summary().environmentsCreated()).isEqualTo(1);
        assertThat(result.summary().serversCreated()).isEqualTo(1);
        assertThat(result.summary().tomcatTargetsCreated()).isEqualTo(1);
        assertThat(result.summary().actuatorTargetsCreated()).isEqualTo(1);
        assertThat(result.summary().tomcatExpectedWebappSpecsApplied()).isEqualTo(1);
        assertThat(result.summary().tomcatExpectedWebappItemsApplied()).isEqualTo(2);
        assertThat(result.validation().valid()).isTrue();
        assertThat(result.objects()).extracting("objectType")
                .contains(
                        ProvisioningPlanObjectTypeDto.ENVIRONMENT,
                        ProvisioningPlanObjectTypeDto.SERVER,
                        ProvisioningPlanObjectTypeDto.TOMCAT_TARGET,
                        ProvisioningPlanObjectTypeDto.ACTUATOR_TARGET,
                        ProvisioningPlanObjectTypeDto.TOMCAT_EXPECTED_WEBAPPS
                );

        ArgumentCaptor<TomcatTargetEntity> tomcatCaptor = ArgumentCaptor.forClass(TomcatTargetEntity.class);
        verify(tomcatTargetRepository).save(tomcatCaptor.capture());
        assertThat(tomcatCaptor.getValue().getPassword()).isEqualTo("hc-manager-pass");
        assertThat(tomcatCaptor.getValue().getBaseUrl()).isEqualTo("http://nft03-tomcats.internal");

        ArgumentCaptor<TomcatExpectedWebappsSpecReplaceRequestDto> expectedCaptor =
                ArgumentCaptor.forClass(TomcatExpectedWebappsSpecReplaceRequestDto.class);
        verify(tomcatExpectedWebappsSpecService).replaceForServer(any(), any(), expectedCaptor.capture());
        assertThat(expectedCaptor.getValue().specs()).hasSize(1);
        assertThat(expectedCaptor.getValue().specs().getFirst().serverId()).isNotNull();
        assertThat(expectedCaptor.getValue().specs().getFirst().items()).containsExactly("/payments", "/payments-admin");
    }

    @Test
    void rejectsInvalidPlanWithoutSaving() {
        EnvironmentProvisioningPlanDto plan = createPlan();
        when(validationService.validate(plan)).thenReturn(new ProvisioningPlanValidationResultDto(
                false,
                List.of(new ProvisioningPlanIssueDto(ProvisioningPlanIssueSeverityDto.ERROR, "/environment/name", "environment.name already exists")),
                List.of(),
                List.of()
        ));

        assertThatThrownBy(() -> service.apply(new ProvisioningPlanApplyRequestDto(plan, false)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Provisioning plan is invalid");

        verify(environmentRepository, never()).save(any());
        verify(serverRepository, never()).save(any());
        verify(tomcatTargetRepository, never()).save(any());
        verify(actuatorTargetRepository, never()).save(any());
    }

    @Test
    void rejectsScanAfterApplyUntilExplicitlyImplemented() {
        assertThatThrownBy(() -> service.apply(new ProvisioningPlanApplyRequestDto(createPlan(), true)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("scanAfterApply is not supported yet");

        verify(validationService, never()).validate(any());
    }

    private static ProvisioningPlanValidationResultDto valid() {
        return new ProvisioningPlanValidationResultDto(true, List.of(), List.of(), List.of());
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
                        List.of(new ProvisioningTomcatTargetDto(
                                TomcatRole.PAYMENTS,
                                TargetAdapterTypeDto.TOMCAT_MANAGER_HTML,
                                "http://nft03-tomcats.internal",
                                8081,
                                "hc-manager",
                                "hc-manager-pass",
                                1500,
                                5000
                        )),
                        List.of(new ProvisioningActuatorTargetDto(
                                TomcatRole.PAYMENTS,
                                TargetAdapterTypeDto.ACTUATOR_HTTP,
                                "http://nft03-services.internal",
                                8080,
                                "payments",
                                1500,
                                5000
                        )),
                        new ProvisioningTomcatExpectedWebappsDto(
                                ProvisioningExpectedSetChangeModeDto.REPLACE,
                                List.of(new ProvisioningTomcatExpectedWebappsSpecDto(
                                        TomcatRole.PAYMENTS,
                                        ExpectedSetMode.EXPLICIT,
                                        null,
                                        List.of("/payments", "/payments-admin")
                                ))
                        ),
                        new ProvisioningDockerExpectedServicesDto(
                                ProvisioningExpectedSetChangeModeDto.NO_CHANGE,
                                List.of()
                        )
                ))
        );
    }
}
