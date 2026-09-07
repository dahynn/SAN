package com.san.api.domain.auth.service;

import com.san.api.global.audit.context.AuditRequestContext;
import com.san.api.global.audit.context.AuditRequestContextHolder;
import com.san.api.global.audit.dto.AuditLogCreateCommand;
import com.san.api.global.audit.entity.AuditEventDomain;
import com.san.api.global.audit.entity.AuditEventType;
import com.san.api.global.audit.entity.AuditOutcome;
import com.san.api.global.audit.service.AuditLogService;
import com.san.api.global.exception.errorcode.AuthErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

/**
 * AuthAuditService가 요청 컨텍스트를 감사 이벤트에 포함하고,
 * 감사 저장 실패를 인증 흐름으로 전파하지 않는지 검증하는 테스트.
 */
@ExtendWith(MockitoExtension.class)
class AuthAuditServiceTest {

    @Mock
    private AuditLogService auditLogService;

    @AfterEach
    void tearDown() {
        AuditRequestContextHolder.clear();
    }

    @Test
    void recordSuccessIncludesRequestContext() {
        AuthAuditService authAuditService = new AuthAuditService(auditLogService);
        UUID userId = UUID.randomUUID();
        AuditRequestContextHolder.set(new AuditRequestContext(
                "trace-1",
                "203.0.113.10",
                "Mozilla/5.0"
        ));

        authAuditService.recordSuccess(
                userId,
                AuditEventType.LOGIN_SUCCESS,
                userId,
                Map.of("clientType", "DASHBOARD")
        );

        ArgumentCaptor<AuditLogCreateCommand> captor = ArgumentCaptor.forClass(AuditLogCreateCommand.class);
        verify(auditLogService).save(captor.capture());
        AuditLogCreateCommand command = captor.getValue();
        assertThat(command.actorUserId()).isEqualTo(userId);
        assertThat(command.eventDomain()).isEqualTo(AuditEventDomain.AUTH);
        assertThat(command.eventType()).isEqualTo(AuditEventType.LOGIN_SUCCESS);
        assertThat(command.outcome()).isEqualTo(AuditOutcome.SUCCESS);
        assertThat(command.traceId()).isEqualTo("trace-1");
        assertThat(command.ipAddress()).isEqualTo("203.0.113.10");
        assertThat(command.userAgent()).isEqualTo("Mozilla/5.0");
        assertThat(command.metadata()).containsEntry("clientType", "DASHBOARD");
    }

    @Test
    void recordFailureDoesNotPropagateAuditSaveFailure() {
        AuthAuditService authAuditService = new AuthAuditService(auditLogService);
        doThrow(new IllegalStateException("db down"))
                .when(auditLogService)
                .save(org.mockito.ArgumentMatchers.any(AuditLogCreateCommand.class));

        assertThatCode(() -> authAuditService.recordFailure(
                null,
                AuditEventType.LOGIN_FAILURE,
                AuthErrorCode.INVALID_CREDENTIALS,
                Map.of("username", "dahyeon")
        )).doesNotThrowAnyException();
    }

    @Test
    void recordFailureUsesAuditFailureReasonAndPreservesClientErrorCode() {
        AuthAuditService authAuditService = new AuthAuditService(auditLogService);
        UUID targetUserId = UUID.randomUUID();

        authAuditService.recordFailure(
                null,
                AuditEventType.LOGIN_FAILURE,
                targetUserId,
                AuthErrorCode.INVALID_CREDENTIALS,
                Map.of("username", "dahyeon")
        );

        ArgumentCaptor<AuditLogCreateCommand> captor = ArgumentCaptor.forClass(AuditLogCreateCommand.class);
        verify(auditLogService).save(captor.capture());
        AuditLogCreateCommand command = captor.getValue();
        assertThat(command.actorUserId()).isNull();
        assertThat(command.targetId()).isEqualTo(targetUserId);
        assertThat(command.failureReasonCode()).isEqualTo("AUTH.INVALID_CREDENTIALS");
        assertThat(command.failureMessage()).isEqualTo("아이디 또는 비밀번호가 올바르지 않습니다.");
        assertThat(command.metadata())
                .containsEntry("username", "dahyeon")
                .containsEntry("clientErrorCode", "A002")
                .containsEntry("httpStatus", 401);
    }
}
