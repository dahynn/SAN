package com.san.api.global.audit.service;

import com.san.api.global.config.ObjectMapperConfig;
import com.san.api.global.audit.entity.AuditEventDomain;
import com.san.api.global.audit.entity.AuditEventType;
import com.san.api.global.audit.entity.AuditLogEvent;
import com.san.api.global.audit.entity.AuditOutcome;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AuditLogIntegrityHasherTest {

    private final AuditLogIntegrityHasher hasher = new AuditLogIntegrityHasher(new ObjectMapperConfig().objectMapper());

    @Test
    void hashIsStableForEquivalentMetadataOrder() {
        UUID eventId = UUID.randomUUID();
        LocalDateTime occurredAt = LocalDateTime.of(2026, 5, 18, 13, 30);
        AuditLogEvent first = event(Map.of(
                "clientType", "DASHBOARD",
                "failCount", 3
        ));
        Map<String, Object> reorderedMetadata = new LinkedHashMap<>();
        reorderedMetadata.put("failCount", 3);
        reorderedMetadata.put("clientType", "DASHBOARD");
        AuditLogEvent second = event(reorderedMetadata);
        setStableIdentity(first, eventId, occurredAt);
        setStableIdentity(second, eventId, occurredAt);

        assertThat(hasher.hash(first)).isEqualTo(hasher.hash(second));
    }

    @Test
    void hashChangesWhenAuditedValueChanges() {
        UUID eventId = UUID.randomUUID();
        LocalDateTime occurredAt = LocalDateTime.of(2026, 5, 18, 13, 30);
        AuditLogEvent first = event(Map.of("clientType", "DASHBOARD"));
        AuditLogEvent second = event(Map.of("clientType", "EXTENSION"));
        setStableIdentity(first, eventId, occurredAt);
        setStableIdentity(second, eventId, occurredAt);

        assertThat(hasher.hash(first)).isNotEqualTo(hasher.hash(second));
    }

    private AuditLogEvent event(Map<String, Object> metadata) {
        return AuditLogEvent.builder()
                .traceId("trace-integrity")
                .eventDomain(AuditEventDomain.AUTH)
                .eventType(AuditEventType.LOGIN_FAILURE)
                .targetType("USER")
                .outcome(AuditOutcome.FAILURE)
                .failureReasonCode("AUTH.INVALID_CREDENTIALS")
                .failureMessage("아이디 또는 비밀번호가 올바르지 않습니다.")
                .ipAddress("203.0.113.10")
                .userAgent("Mozilla/5.0")
                .metadata(metadata)
                .build();
    }

    private void setStableIdentity(AuditLogEvent event, UUID eventId, LocalDateTime occurredAt) {
        ReflectionTestUtils.setField(event, "auditLogEventId", eventId);
        ReflectionTestUtils.setField(event, "occurredAt", occurredAt);
    }
}
