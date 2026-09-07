package com.san.api.domain.github.service;

import com.san.api.global.audit.context.AuditRequestContext;
import com.san.api.global.audit.context.AuditRequestContextHolder;
import com.san.api.global.audit.dto.AuditLogCreateCommand;
import com.san.api.global.audit.entity.AuditEventDomain;
import com.san.api.global.audit.entity.AuditEventType;
import com.san.api.global.audit.entity.AuditOutcome;
import com.san.api.global.audit.entity.AuditTargetType;
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
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GithubAuditServiceTest {

    @Mock
    private AuditLogService auditLogService;

    @AfterEach
    void tearDown() {
        AuditRequestContextHolder.clear();
    }

    @Test
    void recordSuccessCreatesGithubAuditCommandWithRequestContext() {
        GithubAuditService githubAuditService = new GithubAuditService(auditLogService);
        UUID userId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        AuditRequestContextHolder.set(new AuditRequestContext(
                "trace-github",
                "203.0.113.10",
                "Mozilla/5.0"
        ));

        githubAuditService.recordSuccess(
                userId,
                AuditEventType.GITHUB_TOKEN_LINKED,
                AuditTargetType.GITHUB_ACCOUNT,
                targetId,
                Map.of("operation", "LINK_ACCOUNT")
        );

        ArgumentCaptor<AuditLogCreateCommand> captor = ArgumentCaptor.forClass(AuditLogCreateCommand.class);
        verify(auditLogService).save(captor.capture());
        AuditLogCreateCommand command = captor.getValue();
        assertThat(command.actorUserId()).isEqualTo(userId);
        assertThat(command.eventDomain()).isEqualTo(AuditEventDomain.GITHUB);
        assertThat(command.eventType()).isEqualTo(AuditEventType.GITHUB_TOKEN_LINKED);
        assertThat(command.targetType()).isEqualTo("GITHUB_ACCOUNT");
        assertThat(command.targetId()).isEqualTo(targetId);
        assertThat(command.outcome()).isEqualTo(AuditOutcome.SUCCESS);
        assertThat(command.traceId()).isEqualTo("trace-github");
        assertThat(command.metadata()).containsEntry("operation", "LINK_ACCOUNT");
    }

    @Test
    void recordFailureIncludesErrorCodeMetadata() {
        GithubAuditService githubAuditService = new GithubAuditService(auditLogService);
        UUID userId = UUID.randomUUID();

        githubAuditService.recordFailure(
                userId,
                AuditEventType.GITHUB_API_FAILED,
                AuditTargetType.GITHUB_REPOSITORY,
                userId,
                AuthErrorCode.GITHUB_REPOSITORY_NOT_FOUND,
                Map.of("operation", "CONNECT_REPOSITORY")
        );

        ArgumentCaptor<AuditLogCreateCommand> captor = ArgumentCaptor.forClass(AuditLogCreateCommand.class);
        verify(auditLogService).save(captor.capture());
        AuditLogCreateCommand command = captor.getValue();
        assertThat(command.outcome()).isEqualTo(AuditOutcome.FAILURE);
        assertThat(command.failureReasonCode()).isEqualTo("GITHUB.A203");
        assertThat(command.metadata())
                .containsEntry("operation", "CONNECT_REPOSITORY")
                .containsEntry("clientErrorCode", "A203")
                .containsEntry("httpStatus", 404);
    }
}
