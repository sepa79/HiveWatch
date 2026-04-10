package io.pockethive.hivewatch.service.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.pockethive.hivewatch.service.api.EnvironmentProvisioningPlanDto;
import io.pockethive.hivewatch.service.api.ExpectedSetMode;
import io.pockethive.hivewatch.service.api.HiveWatchRole;
import io.pockethive.hivewatch.service.api.ProvisioningActuatorTargetDto;
import io.pockethive.hivewatch.service.api.ProvisioningAppliedObjectDto;
import io.pockethive.hivewatch.service.api.ProvisioningApplySummaryDto;
import io.pockethive.hivewatch.service.api.ProvisioningChangeModeDto;
import io.pockethive.hivewatch.service.api.ProvisioningDockerExpectedServicesDto;
import io.pockethive.hivewatch.service.api.ProvisioningEnvironmentDto;
import io.pockethive.hivewatch.service.api.ProvisioningExpectedSetChangeModeDto;
import io.pockethive.hivewatch.service.api.ProvisioningPlanObjectTypeDto;
import io.pockethive.hivewatch.service.api.ProvisioningServerDto;
import io.pockethive.hivewatch.service.api.ProvisioningTomcatExpectedWebappsDto;
import io.pockethive.hivewatch.service.api.ProvisioningTomcatExpectedWebappsSpecDto;
import io.pockethive.hivewatch.service.api.ProvisioningTomcatTargetDto;
import io.pockethive.hivewatch.service.api.TargetAdapterTypeDto;
import io.pockethive.hivewatch.service.api.TomcatRole;
import io.pockethive.hivewatch.service.security.CurrentUserService;
import io.pockethive.hivewatch.service.security.HiveWatchPrincipal;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConfigAuditServiceTest {
    private final ConfigRevisionRepository configRevisionRepository = mock(ConfigRevisionRepository.class);
    private final AuditEventRepository auditEventRepository = mock(AuditEventRepository.class);
    private final CurrentUserService currentUserService = mock(CurrentUserService.class);
    private final ConfigAuditService service = new ConfigAuditService(
            configRevisionRepository,
            auditEventRepository,
            currentUserService,
            new ObjectMapper()
    );

    @Test
    void recordsProvisioningRevisionWithRedactedPlanAndObjectEvents() {
        UUID actorId = UUID.randomUUID();
        UUID environmentId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        when(currentUserService.requirePrincipal()).thenReturn(new HiveWatchPrincipal(
                actorId,
                "alice",
                "Alice",
                Set.of(HiveWatchRole.ADMIN)
        ));
        when(configRevisionRepository.save(any(ConfigRevisionEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.recordProvisioningApply(
                environmentId,
                plan(),
                new ProvisioningApplySummaryDto(1, 1, 1, 1, 1, 1, 0, 0),
                List.of(new ProvisioningAppliedObjectDto(
                        ProvisioningPlanObjectTypeDto.TOMCAT_TARGET,
                        "touchpoint",
                        targetId,
                        "TOMCAT_MANAGER_HTML PAYMENTS http://nft03-tomcats.internal:8081"
                ))
        );

        ArgumentCaptor<ConfigRevisionEntity> revisionCaptor = ArgumentCaptor.forClass(ConfigRevisionEntity.class);
        verify(configRevisionRepository).save(revisionCaptor.capture());
        ConfigRevisionEntity revision = revisionCaptor.getValue();
        assertThat(revision.getEnvironmentId()).isEqualTo(environmentId);
        assertThat(revision.getActorUserId()).isEqualTo(actorId);
        assertThat(revision.getActorUsername()).isEqualTo("alice");
        assertThat(revision.getSource()).isEqualTo("UI");
        assertThat(revision.getPlanHash()).hasSize(64);
        assertThat(revision.getPlanJson()).contains("<redacted>");
        assertThat(revision.getPlanJson()).doesNotContain("secret-password");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Iterable<AuditEventEntity>> eventsCaptor = ArgumentCaptor.forClass(Iterable.class);
        verify(auditEventRepository).saveAll(eventsCaptor.capture());
        List<AuditEventEntity> events = toList(eventsCaptor.getValue());
        assertThat(events).hasSize(1);
        assertThat(events.getFirst().getRevisionId()).isEqualTo(revision.getId());
        assertThat(events.getFirst().getObjectType()).isEqualTo("TOMCAT_TARGET");
        assertThat(events.getFirst().getObjectId()).isEqualTo(targetId);
        assertThat(events.getFirst().getAction()).isEqualTo("PROVISIONING_APPLY");
    }

    private static EnvironmentProvisioningPlanDto plan() {
        return new EnvironmentProvisioningPlanDto(
                "UI",
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
                                "secret-password",
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
                                        List.of("/payments")
                                ))
                        ),
                        new ProvisioningDockerExpectedServicesDto(
                                ProvisioningExpectedSetChangeModeDto.NO_CHANGE,
                                List.of()
                        )
                ))
        );
    }

    private static List<AuditEventEntity> toList(Iterable<AuditEventEntity> events) {
        java.util.ArrayList<AuditEventEntity> result = new java.util.ArrayList<>();
        events.forEach(result::add);
        return List.copyOf(result);
    }
}
