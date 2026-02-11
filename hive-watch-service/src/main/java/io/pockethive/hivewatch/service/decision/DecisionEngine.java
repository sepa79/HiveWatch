package io.pockethive.hivewatch.service.decision;

import io.pockethive.hivewatch.service.api.DecisionIssueDto;
import io.pockethive.hivewatch.service.api.DecisionIssueKind;
import io.pockethive.hivewatch.service.api.DecisionVerdict;
import io.pockethive.hivewatch.service.api.TomcatScanOutcomeKind;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DecisionEngine {
    private static final double CPU_WARN = 0.75;
    private static final double CPU_BLOCK = 0.90;
    private static final long MEMORY_WARN_BYTES = 900L * 1024L * 1024L;
    private static final long MEMORY_BLOCK_BYTES = 1500L * 1024L * 1024L;

    public DecisionEvaluation evaluate(
            List<DecisionInputs.TomcatTargetObservation> tomcats,
            List<DecisionInputs.ActuatorTargetObservation> actuators
    ) {
        List<DecisionIssueDto> issues = new ArrayList<>();

        for (DecisionInputs.TomcatTargetObservation t : tomcats) {
            if (t.scannedAt() == null || t.outcomeKind() == null) {
                issues.add(new DecisionIssueDto(
                        DecisionVerdict.UNKNOWN,
                        DecisionIssueKind.TOMCAT_TARGET,
                        t.targetId(),
                        t.serverName(),
                        t.role(),
                        "Tomcat " + t.role().name().toLowerCase(),
                        "Not scanned"
                ));
                continue;
            }
            if (t.outcomeKind() == TomcatScanOutcomeKind.ERROR) {
                issues.add(new DecisionIssueDto(
                        DecisionVerdict.BLOCK,
                        DecisionIssueKind.TOMCAT_TARGET,
                        t.targetId(),
                        t.serverName(),
                        t.role(),
                        "Tomcat " + t.role().name().toLowerCase(),
                        (t.errorKind() == null ? "ERROR" : t.errorKind().name()) + ": " + (t.errorMessage() == null ? "Unknown error" : t.errorMessage())
                ));
            }
        }

        for (DecisionInputs.ActuatorTargetObservation a : actuators) {
            if (a.scannedAt() == null || a.outcomeKind() == null) {
                issues.add(new DecisionIssueDto(
                        DecisionVerdict.UNKNOWN,
                        DecisionIssueKind.ACTUATOR_TARGET,
                        a.targetId(),
                        a.serverName(),
                        a.role(),
                        "Microservice " + a.profile(),
                        "Not scanned"
                ));
                continue;
            }
            if (a.outcomeKind() == TomcatScanOutcomeKind.ERROR) {
                issues.add(new DecisionIssueDto(
                        DecisionVerdict.BLOCK,
                        DecisionIssueKind.ACTUATOR_TARGET,
                        a.targetId(),
                        a.serverName(),
                        a.role(),
                        "Microservice " + a.profile(),
                        (a.errorKind() == null ? "ERROR" : a.errorKind().name()) + ": " + (a.errorMessage() == null ? "Unknown error" : a.errorMessage())
                ));
                continue;
            }

            DecisionVerdict severity = DecisionVerdict.OK;
            List<String> parts = new ArrayList<>();

            String hs = a.healthStatus() == null ? "" : a.healthStatus().trim();
            if (!"UP".equalsIgnoreCase(hs)) {
                severity = DecisionVerdict.BLOCK;
                parts.add("health=" + (hs.isBlank() ? "UNKNOWN" : hs.toUpperCase()));
            }

            if (a.cpuUsage() != null) {
                if (a.cpuUsage() >= CPU_BLOCK) {
                    severity = DecisionVerdict.BLOCK;
                    parts.add("cpu=" + formatPct(a.cpuUsage()));
                } else if (a.cpuUsage() >= CPU_WARN && severity != DecisionVerdict.BLOCK) {
                    severity = DecisionVerdict.WARN;
                    parts.add("cpu=" + formatPct(a.cpuUsage()));
                }
            }

            if (a.memoryUsedBytes() != null) {
                if (a.memoryUsedBytes() >= MEMORY_BLOCK_BYTES) {
                    severity = DecisionVerdict.BLOCK;
                    parts.add("memory=" + formatBytes(a.memoryUsedBytes()));
                } else if (a.memoryUsedBytes() >= MEMORY_WARN_BYTES && severity != DecisionVerdict.BLOCK) {
                    severity = DecisionVerdict.WARN;
                    parts.add("memory=" + formatBytes(a.memoryUsedBytes()));
                }
            }

            if (severity != DecisionVerdict.OK) {
                String message = String.join(", ", parts);
                if (a.appName() != null && !a.appName().isBlank()) {
                    message = a.appName().trim() + (message.isBlank() ? "" : " · " + message);
                }
                issues.add(new DecisionIssueDto(
                        severity,
                        DecisionIssueKind.ACTUATOR_TARGET,
                        a.targetId(),
                        a.serverName(),
                        a.role(),
                        "Microservice " + a.profile(),
                        message.isBlank() ? "Degraded" : message
                ));
            }
        }

        int block = 0;
        int warn = 0;
        int unknown = 0;
        for (DecisionIssueDto i : issues) {
            switch (i.severity()) {
                case BLOCK -> block++;
                case WARN -> warn++;
                case UNKNOWN -> unknown++;
                case OK -> {
                }
            }
        }

        DecisionVerdict verdict = DecisionVerdict.OK;
        if (block > 0) {
            verdict = DecisionVerdict.BLOCK;
        } else if (warn > 0) {
            verdict = DecisionVerdict.WARN;
        } else if (unknown > 0) {
            verdict = DecisionVerdict.UNKNOWN;
        }

        return new DecisionEvaluation(verdict, List.copyOf(issues), block, warn, unknown);
    }

    private static String formatPct(double ratio) {
        double pct = Math.round(ratio * 1000.0) / 10.0;
        return pct + "%";
    }

    private static String formatBytes(long bytes) {
        double mb = bytes / (1024.0 * 1024.0);
        if (mb < 1024.0) {
            return Math.round(mb) + "MB";
        }
        double gb = mb / 1024.0;
        double rounded = Math.round(gb * 10.0) / 10.0;
        return rounded + "GB";
    }
}

