package com.san.api.domain.til.service;

import com.san.api.global.audit.context.AuditRequestContext;
import com.san.api.global.audit.context.AuditRequestContextHolder;
import com.san.api.global.audit.dto.AuditLogCreateCommand;
import com.san.api.global.audit.entity.AuditEventDomain;
import com.san.api.global.audit.entity.AuditEventType;
import com.san.api.global.audit.entity.AuditOutcome;
import com.san.api.global.audit.entity.AuditTargetType;
import com.san.api.global.audit.service.AuditLogService;
import com.san.api.global.exception.errorcode.TilErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TilAuditServiceTest {

    @Mock
    private AuditLogService auditLogService;

    @AfterEach
    void tearDown() {
        AuditRequestContextHolder.clear();
    }

    @Test
    void recordSuccessCreatesTilAuditCommandWithRequestContext() {
        TilAuditService tilAuditService = new TilAuditService(auditLogService);
        UUID userId = UUID.randomUUID();
        UUID commitId = UUID.randomUUID();
        AuditRequestContextHolder.set(new AuditRequestContext(
                "trace-til",
                "203.0.113.10",
                "Mozilla/5.0"
        ));

        tilAuditService.recordSuccess(
                userId,
                AuditEventType.TIL_COMMIT_REQUESTED,
                AuditTargetType.TIL_GITHUB_COMMIT,
                commitId,
                Map.of("branch", "main")
        );

        ArgumentCaptor<AuditLogCreateCommand> captor = ArgumentCaptor.forClass(AuditLogCreateCommand.class);
        verify(auditLogService).save(captor.capture());
        AuditLogCreateCommand command = captor.getValue();
        assertThat(command.actorUserId()).isEqualTo(userId);
        assertThat(command.eventDomain()).isEqualTo(AuditEventDomain.TIL);
        assertThat(command.eventType()).isEqualTo(AuditEventType.TIL_COMMIT_REQUESTED);
        assertThat(command.targetType()).isEqualTo("TIL_GITHUB_COMMIT");
        assertThat(command.targetId()).isEqualTo(commitId);
        assertThat(command.outcome()).isEqualTo(AuditOutcome.SUCCESS);
        assertThat(command.traceId()).isEqualTo("trace-til");
        assertThat(command.metadata()).containsEntry("branch", "main");
    }

    @Test
    void recordFailureIncludesErrorCodeMetadata() {
        TilAuditService tilAuditService = new TilAuditService(auditLogService);
        UUID userId = UUID.randomUUID();
        UUID summaryId = UUID.randomUUID();

        tilAuditService.recordFailure(
                userId,
                AuditEventType.TIL_COMMIT_DUPLICATE_BLOCKED,
                AuditTargetType.DAILY_SUMMARY,
                summaryId,
                TilErrorCode.TIL_ALREADY_COMMITTED,
                Map.of("summaryId", summaryId)
        );

        ArgumentCaptor<AuditLogCreateCommand> captor = ArgumentCaptor.forClass(AuditLogCreateCommand.class);
        verify(auditLogService).save(captor.capture());
        AuditLogCreateCommand command = captor.getValue();
        assertThat(command.outcome()).isEqualTo(AuditOutcome.FAILURE);
        assertThat(command.failureReasonCode()).isEqualTo("TIL.T007");
        assertThat(command.metadata())
                .containsEntry("summaryId", summaryId)
                .containsEntry("clientErrorCode", "T007")
                .containsEntry("httpStatus", 409);
    }
}
