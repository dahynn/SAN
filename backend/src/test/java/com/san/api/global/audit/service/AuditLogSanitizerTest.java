package com.san.api.global.audit.service;

import com.san.api.global.audit.dto.AuditLogCreateCommand;
import com.san.api.global.audit.entity.AuditEventDomain;
import com.san.api.global.audit.entity.AuditEventType;
import com.san.api.global.audit.entity.AuditOutcome;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AuditLogSanitizerTest {

    @Test
    void masksSensitiveMetadataAndFailureMessage() {
        AuditLogCreateCommand command = new AuditLogCreateCommand(
                null,
                "trace-1",
                AuditEventDomain.AUTH,
                AuditEventType.LOGIN_FAILURE,
                "USER",
                null,
                AuditOutcome.FAILURE,
                " invalid credentials ",
                "login failed for user@example.com with Bearer raw-token",
                "203.0.113.10",
                "Mozilla/5.0",
                Map.of(
                        "username", "dahyeon",
                        "email", "user@example.com",
                        "refreshToken", "raw-refresh-token",
                        "nested", Map.of("sessionId", "session-1"),
                        "items", List.of(Map.of("password", "secret"), "contact me at owner@example.com")
                )
        );

        AuditLogCreateCommand sanitized = AuditLogSanitizer.sanitize(command);

        assertThat(sanitized.failureReasonCode()).isEqualTo("INVALID_CREDENTIALS");
        assertThat(sanitized.failureMessage()).isEqualTo(
                "login failed for us***@example.com with Bearer [REDACTED]"
        );
        assertThat(sanitized.metadata())
                .containsEntry("username", "da***")
                .containsEntry("email", "us***@example.com")
                .containsEntry("refreshToken", "[REDACTED]");
        Map<?, ?> nested = (Map<?, ?>) sanitized.metadata().get("nested");
        assertThat(nested.get("sessionId")).isEqualTo("[REDACTED]");

        List<?> items = (List<?>) sanitized.metadata().get("items");
        assertThat(items).hasSize(2);
        assertThat(items.get(0)).isEqualTo(Map.of("password", "[REDACTED]"));
        assertThat(items.get(1)).isEqualTo("contact me at ow***@example.com");
    }

    @Test
    void standardizesMissingFailureReason() {
        AuditLogCreateCommand command = new AuditLogCreateCommand(
                null,
                "trace-1",
                AuditEventDomain.AUTH,
                AuditEventType.LOGIN_FAILURE,
                "USER",
                null,
                AuditOutcome.FAILURE,
                null,
                null,
                null,
                null,
                Map.of()
        );

        AuditLogCreateCommand sanitized = AuditLogSanitizer.sanitize(command);

        assertThat(sanitized.failureReasonCode()).isEqualTo("UNKNOWN_FAILURE");
        assertThat(sanitized.failureMessage()).isEqualTo("Audit failure occurred.");
    }

    @Test
    void clearsFailureFieldsForSuccessOutcome() {
        AuditLogCreateCommand command = new AuditLogCreateCommand(
                null,
                "trace-1",
                AuditEventDomain.AUTH,
                AuditEventType.LOGIN_SUCCESS,
                "USER",
                null,
                AuditOutcome.SUCCESS,
                "SHOULD_NOT_STAY",
                "should not stay",
                null,
                null,
                Map.of("clientType", "DASHBOARD")
        );

        AuditLogCreateCommand sanitized = AuditLogSanitizer.sanitize(command);

        assertThat(sanitized.failureReasonCode()).isNull();
        assertThat(sanitized.failureMessage()).isNull();
        assertThat(sanitized.metadata()).containsEntry("clientType", "DASHBOARD");
    }
}
