package io.pockethive.hivewatch.service.decision;

import io.pockethive.hivewatch.service.api.DecisionVerdict;
import io.pockethive.hivewatch.service.api.TomcatRole;
import io.pockethive.hivewatch.service.api.TomcatScanErrorKind;
import io.pockethive.hivewatch.service.api.TomcatScanOutcomeKind;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DecisionEngineTest {
    private final DecisionEngine engine = new DecisionEngine();

    @Test
    void blocksOnActuatorDown() {
        DecisionEvaluation evaluation = engine.evaluate(
                List.of(),
                List.of(new DecisionInputs.ActuatorTargetObservation(
                        UUID.randomUUID(),
                        "Docker Swarm",
                        TomcatRole.SERVICES,
                        "http://example",
                        8080,
                        "services",
                        Instant.now(),
                        TomcatScanOutcomeKind.SUCCESS,
                        null,
                        null,
                        "DOWN",
                        "services-service",
                        0.1,
                        100L
                ))
        );

        assertEquals(DecisionVerdict.BLOCK, evaluation.verdict());
        assertEquals(1, evaluation.blockIssues());
    }

    @Test
    void warnsOnHighCpu() {
        DecisionEvaluation evaluation = engine.evaluate(
                List.of(),
                List.of(new DecisionInputs.ActuatorTargetObservation(
                        UUID.randomUUID(),
                        "Docker Swarm",
                        TomcatRole.SERVICES,
                        "http://example",
                        8080,
                        "services",
                        Instant.now(),
                        TomcatScanOutcomeKind.SUCCESS,
                        null,
                        null,
                        "UP",
                        "services-service",
                        0.80,
                        100L
                ))
        );

        assertEquals(DecisionVerdict.WARN, evaluation.verdict());
        assertEquals(1, evaluation.warnIssues());
    }

    @Test
    void unknownIfNotScannedAndNoWarnOrBlock() {
        DecisionEvaluation evaluation = engine.evaluate(
                List.of(new DecisionInputs.TomcatTargetObservation(
                        UUID.randomUUID(),
                        "Touchpoint",
                        TomcatRole.AUTH,
                        "http://example",
                        8081,
                        null,
                        null,
                        null,
                        null
                )),
                List.of()
        );

        assertEquals(DecisionVerdict.UNKNOWN, evaluation.verdict());
        assertEquals(1, evaluation.unknownIssues());
    }

    @Test
    void blocksOnTomcatScanError() {
        DecisionEvaluation evaluation = engine.evaluate(
                List.of(new DecisionInputs.TomcatTargetObservation(
                        UUID.randomUUID(),
                        "Touchpoint",
                        TomcatRole.AUTH,
                        "http://example",
                        8081,
                        Instant.now(),
                        TomcatScanOutcomeKind.ERROR,
                        TomcatScanErrorKind.TIMEOUT,
                        "Timeout"
                )),
                List.of()
        );

        assertEquals(DecisionVerdict.BLOCK, evaluation.verdict());
        assertEquals(1, evaluation.blockIssues());
    }
}

