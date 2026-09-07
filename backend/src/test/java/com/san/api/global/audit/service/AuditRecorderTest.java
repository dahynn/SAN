package com.san.api.global.audit.service;

import com.san.api.global.audit.context.AuditRequestContext;
import com.san.api.global.audit.context.AuditRequestContextHolder;
import com.san.api.global.audit.dto.AuditLogCreateCommand;
import com.san.api.global.audit.dto.AuditRecordCommand;
import com.san.api.global.audit.entity.AuditEventDomain;
import com.san.api.global.audit.entity.AuditEventType;
import com.san.api.global.audit.entity.AuditOutcome;
import com.san.api.global.exception.errorcode.CommonErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuditRecorderTest {

    private final AuditLogService auditLogService = mock(AuditLogService.class);
    private final AuditRecorder auditRecorder = new AuditRecorder(auditLogService);

    @AfterEach
    void tearDown() {
        AuditRequestContextHolder.clear();
    }

    @Test
    void recordSuccessUsesCurrentRequestContextWhenCommandContextIsMissing() {
        UUID actorUserId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        UUID auditId = UUID.randomUUID();
        AuditRequestContextHolder.set(new AuditRequestContext("trace-1", "203.0.113.10", "JUnit"));
        when(auditLogService.save(any(AuditLogCreateCommand.class))).thenReturn(auditId);

        UUID result = auditRecorder.recordSuccess(AuditRecordCommand.builder()
                .actorUserId(actorUserId)
                .eventDomain(AuditEventDomain.OUTBOX)
                .eventType(AuditEventType.OUTBOX_EVENT_RETRY_REQUESTED)
                .targetType("OUTBOX_EVENT")
                .targetId(targetId)
                .metadata(Map.of("retryCount", 1))
                .build());

        ArgumentCaptor<AuditLogCreateCommand> captor = ArgumentCaptor.forClass(AuditLogCreateCommand.class);
        verify(auditLogService).save(captor.capture());
        AuditLogCreateCommand command = captor.getValue();
        assertThat(result).isEqualTo(auditId);
        assertThat(command.actorUserId()).isEqualTo(actorUserId);
        assertThat(command.traceId()).isEqualTo("trace-1");
        assertThat(command.ipAddress()).isEqualTo("203.0.113.10");
        assertThat(command.userAgent()).isEqualTo("JUnit");
        assertThat(command.outcome()).isEqualTo(AuditOutcome.SUCCESS);
        assertThat(command.metadata()).containsEntry("retryCount", 1);
    }

    @Test
    void recordFailureAddsErrorCodeMetadata() {
        UUID targetId = UUID.randomUUID();
        when(auditLogService.save(any(AuditLogCreateCommand.class))).thenReturn(UUID.randomUUID());

        auditRecorder.recordFailure(AuditRecordCommand.builder()
                .eventDomain(AuditEventDomain.AUTH)
                .eventType(AuditEventType.LOGIN_FAILURE)
                .targetType("USER")
                .targetId(targetId)
                .metadata(Map.of("clientType", "DASHBOARD"))
                .build(), CommonErrorCode.UNAUTHORIZED);

        ArgumentCaptor<AuditLogCreateCommand> captor = ArgumentCaptor.forClass(AuditLogCreateCommand.class);
        verify(auditLogService).save(captor.capture());
        AuditLogCreateCommand command = captor.getValue();
        assertThat(command.outcome()).isEqualTo(AuditOutcome.FAILURE);
        assertThat(command.failureReasonCode()).isEqualTo("C003");
        assertThat(command.failureMessage()).isEqualTo(CommonErrorCode.UNAUTHORIZED.getMessage());
        assertThat(command.metadata())
                .containsEntry("clientType", "DASHBOARD")
                .containsEntry("clientErrorCode", "C003")
                .containsEntry("httpStatus", 401);
    }

    @Test
    void safeRecordReturnsEmptyWhenAuditSaveFails() {
        doThrow(new RuntimeException("db down")).when(auditLogService).save(any(AuditLogCreateCommand.class));

        Optional<UUID> result = auditRecorder.recordSuccessSafely(AuditRecordCommand.builder()
                .eventDomain(AuditEventDomain.OUTBOX)
                .eventType(AuditEventType.OUTBOX_EVENT_RETRY_REQUESTED)
                .targetType("OUTBOX_EVENT")
                .targetId(UUID.randomUUID())
                .build());

        assertThat(result).isEmpty();
    }
}
