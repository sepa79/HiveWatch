package io.pockethive.hivewatch.service.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.pockethive.hivewatch.service.api.EnvironmentProvisioningPlanDto;
import io.pockethive.hivewatch.service.api.ProvisioningAppliedObjectDto;
import io.pockethive.hivewatch.service.api.ProvisioningApplySummaryDto;
import io.pockethive.hivewatch.service.security.CurrentUserService;
import io.pockethive.hivewatch.service.security.HiveWatchPrincipal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConfigAuditService {
    private static final String PROVISIONING_REVISION_TYPE = "ENVIRONMENT_PROVISIONING";
    private static final String PROVISIONING_ACTION = "PROVISIONING_APPLY";

    private final ConfigRevisionRepository configRevisionRepository;
    private final AuditEventRepository auditEventRepository;
    private final CurrentUserService currentUserService;
    private final ObjectMapper objectMapper;

    public ConfigAuditService(
            ConfigRevisionRepository configRevisionRepository,
            AuditEventRepository auditEventRepository,
            CurrentUserService currentUserService,
            ObjectMapper objectMapper
    ) {
        this.configRevisionRepository = configRevisionRepository;
        this.auditEventRepository = auditEventRepository;
        this.currentUserService = currentUserService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ConfigRevisionEntity recordProvisioningApply(
            UUID environmentId,
            EnvironmentProvisioningPlanDto plan,
            ProvisioningApplySummaryDto summary,
            List<ProvisioningAppliedObjectDto> objects
    ) {
        HiveWatchPrincipal actor = currentUserService.requirePrincipal();
        Instant now = Instant.now();
        String source = requiredTrimmed(plan.source(), "plan.source");
        String redactedPlanJson = toJson(redactSecrets(objectMapper.valueToTree(plan)));
        String summaryJson = toJson(objectMapper.valueToTree(summary));

        ConfigRevisionEntity revision = configRevisionRepository.save(new ConfigRevisionEntity(
                UUID.randomUUID(),
                environmentId,
                PROVISIONING_REVISION_TYPE,
                actor.userId(),
                actor.username(),
                source,
                trimmed(plan.correlationId()),
                trimmed(plan.reason()),
                sha256(redactedPlanJson),
                redactedPlanJson,
                summaryJson,
                now
        ));

        List<AuditEventEntity> events = objects.stream()
                .map(object -> new AuditEventEntity(
                        UUID.randomUUID(),
                        revision.getId(),
                        environmentId,
                        actor.userId(),
                        actor.username(),
                        PROVISIONING_ACTION,
                        object.objectType().name(),
                        object.id(),
                        object.label(),
                        source,
                        trimmed(plan.correlationId()),
                        toJson(Map.of(
                                "clientRef", object.clientRef() == null ? "" : object.clientRef(),
                                "reason", trimmed(plan.reason()) == null ? "" : trimmed(plan.reason())
                        )),
                        now
                ))
                .toList();
        auditEventRepository.saveAll(events);
        return revision;
    }

    private JsonNode redactSecrets(JsonNode node) {
        if (node instanceof ObjectNode object) {
            Iterator<String> names = object.fieldNames();
            while (names.hasNext()) {
                String name = names.next();
                if ("password".equalsIgnoreCase(name)) {
                    object.put(name, "<redacted>");
                } else {
                    redactSecrets(object.get(name));
                }
            }
        } else if (node.isArray()) {
            for (JsonNode child : node) {
                redactSecrets(child);
            }
        }
        return node;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize audit payload", e);
        }
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    private static String requiredTrimmed(String raw, String field) {
        String value = trimmed(raw);
        if (value == null) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }

    private static String trimmed(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim();
        return value.isBlank() ? null : value;
    }
}
