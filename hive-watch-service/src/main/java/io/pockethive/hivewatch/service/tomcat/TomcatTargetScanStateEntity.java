package io.pockethive.hivewatch.service.tomcat;

import io.pockethive.hivewatch.service.api.TomcatScanErrorKind;
import io.pockethive.hivewatch.service.api.TomcatScanOutcomeKind;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "hw_tomcat_target_scan_state")
public class TomcatTargetScanStateEntity {
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

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "webapps", nullable = false, columnDefinition = "jsonb")
    private List<String> webapps = new ArrayList<>();

    protected TomcatTargetScanStateEntity() {
    }

    public TomcatTargetScanStateEntity(
            UUID targetId,
            Instant scannedAt,
            TomcatScanOutcomeKind outcomeKind,
            TomcatScanErrorKind errorKind,
            String errorMessage,
            List<String> webapps
    ) {
        this.targetId = targetId;
        this.scannedAt = scannedAt;
        this.outcomeKind = outcomeKind;
        this.errorKind = errorKind;
        this.errorMessage = errorMessage;
        this.webapps = webapps == null ? new ArrayList<>() : new ArrayList<>(webapps);
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

    public List<String> getWebapps() {
        return List.copyOf(webapps);
    }
}

