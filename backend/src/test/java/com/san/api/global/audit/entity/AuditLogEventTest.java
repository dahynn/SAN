package com.san.api.global.audit.entity;

import org.junit.jupiter.api.Test;

import java.time.temporal.ChronoUnit;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AuditLogEventTest {

    @Test
    void occurredAtUsesDatabaseTimestampPrecision() {
        AuditLogEvent event = AuditLogEvent.builder()
                .traceId("trace-integrity")
                .eventDomain(AuditEventDomain.AUTH)
                .eventType(AuditEventType.LOGIN_FAILURE)
                .targetType("USER")
                .outcome(AuditOutcome.FAILURE)
                .failureReasonCode("AUTH.INVALID_CREDENTIALS")
                .failureMessage("아이디 또는 비밀번호가 올바르지 않습니다.")
                .ipAddress("203.0.113.10")
                .userAgent("Mozilla/5.0")
                .metadata(Map.of("clientType", "DASHBOARD"))
                .build();

        assertThat(event.getOccurredAt())
                .isEqualTo(event.getOccurredAt().truncatedTo(ChronoUnit.MICROS));
    }
}
