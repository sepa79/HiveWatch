package io.pockethive.hivewatch.service.actuator;

import io.pockethive.hivewatch.service.api.TomcatScanErrorKind;
import io.pockethive.hivewatch.service.api.TomcatScanOutcomeKind;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "hw_actuator_target_scan_state")
public class ActuatorTargetScanStateEntity {
    @Id
    @Column(name = "target_id", nullable = false)
    private UUID targetId;

    @Column(name = "scanned_at", nullable = false)
    private Instant scannedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "outcome_kind", nullable = false)
    private TomcatScanOutcomeKind outcomeKind;

    @Enumerated(EnumType.STRING)
    @Column(name = "error_kind")
    private TomcatScanErrorKind errorKind;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "health_status")
    private String healthStatus;

    @Column(name = "app_name")
    private String appName;

    @Column(name = "build_version")
    private String buildVersion;

    @Column(name = "cpu_usage")
    private Double cpuUsage;

    @Column(name = "memory_used_bytes")
    private Long memoryUsedBytes;

    protected ActuatorTargetScanStateEntity() {
    }

    public ActuatorTargetScanStateEntity(
            UUID targetId,
            Instant scannedAt,
            TomcatScanOutcomeKind outcomeKind,
            TomcatScanErrorKind errorKind,
            String errorMessage,
            String healthStatus,
            String appName,
            String buildVersion,
            Double cpuUsage,
            Long memoryUsedBytes
    ) {
        this.targetId = targetId;
        this.scannedAt = scannedAt;
        this.outcomeKind = outcomeKind;
        this.errorKind = errorKind;
        this.errorMessage = errorMessage;
        this.healthStatus = healthStatus;
        this.appName = appName;
        this.buildVersion = buildVersion;
        this.cpuUsage = cpuUsage;
        this.memoryUsedBytes = memoryUsedBytes;
    }

    public UUID getTargetId() {
        return targetId;
    }

    public Instant getScannedAt() {
        return scannedAt;
    }

    public TomcatScanOutcomeKind getOutcomeKind() {
        return outcomeKind;
    }

    public TomcatScanErrorKind getErrorKind() {
        return errorKind;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getHealthStatus() {
        return healthStatus;
    }

    public String getAppName() {
        return appName;
    }

    public String getBuildVersion() {
        return buildVersion;
    }

    public Double getCpuUsage() {
        return cpuUsage;
    }

    public Long getMemoryUsedBytes() {
        return memoryUsedBytes;
    }
}
