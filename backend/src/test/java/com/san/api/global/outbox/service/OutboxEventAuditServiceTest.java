package com.san.api.global.outbox.service;

import com.san.api.global.audit.dto.AuditLogCreateCommand;
import com.san.api.global.audit.entity.AuditEventDomain;
import com.san.api.global.audit.entity.AuditEventType;
import com.san.api.global.audit.entity.AuditOutcome;
import com.san.api.global.audit.entity.AuditTargetType;
import com.san.api.global.audit.service.AuditLogService;
import com.san.api.global.outbox.entity.OutboxEvent;
import com.san.api.global.outbox.entity.OutboxEventType;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class OutboxEventAuditServiceTest {

    @Test
    void recordRetryRequestedUsesAdminAsActorAndOutboxEventAsTarget() {
        AuditLogService auditLogService = mock(AuditLogService.class);
        OutboxEventAuditService service = new OutboxEventAuditService(auditLogService);
        UUID actorUserId = UUID.randomUUID();
        OutboxEvent event = OutboxEvent.builder()
                .eventType(OutboxEventType.FEEDBACK_MATTERMOST_NOTIFICATION)
                .aggregateType("FEEDBACK")
                .aggregateId(UUID.randomUUID())
                .payload(Map.of("feedbackId", UUID.randomUUID().toString()))
                .build();
        ArgumentCaptor<AuditLogCreateCommand> captor = ArgumentCaptor.forClass(AuditLogCreateCommand.class);

        service.recordRetryRequested(actorUserId, event);

        verify(auditLogService).save(captor.capture());
        AuditLogCreateCommand command = captor.getValue();
        assertThat(command.actorUserId()).isEqualTo(actorUserId);
        assertThat(command.targetId()).isEqualTo(event.getOutboxEventId());
        assertThat(command.eventDomain()).isEqualTo(AuditEventDomain.OUTBOX);
        assertThat(command.eventType()).isEqualTo(AuditEventType.OUTBOX_EVENT_RETRY_REQUESTED);
        assertThat(command.targetType()).isEqualTo(AuditTargetType.OUTBOX_EVENT.code());
        assertThat(command.outcome()).isEqualTo(AuditOutcome.SUCCESS);
    }
}
