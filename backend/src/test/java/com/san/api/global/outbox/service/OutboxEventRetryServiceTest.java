package com.san.api.global.outbox.service;

import com.san.api.global.exception.BusinessException;
import com.san.api.global.outbox.dto.response.OutboxEventResponse;
import com.san.api.global.outbox.entity.OutboxEvent;
import com.san.api.global.outbox.entity.OutboxEventStatus;
import com.san.api.global.outbox.entity.OutboxEventType;
import com.san.api.global.outbox.repository.OutboxEventRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OutboxEventRetryServiceTest {

    @Test
    void retryFailedEventChangesStatusToPendingAndRecordsAuditLog() {
        OutboxEventRepository repository = mock(OutboxEventRepository.class);
        OutboxEventAuditService auditService = mock(OutboxEventAuditService.class);
        OutboxEventRetryService service = new OutboxEventRetryService(repository, auditService);
        UUID actorUserId = UUID.randomUUID();
        OutboxEvent event = failedEvent();
        when(repository.findByOutboxEventIdAndStatus(event.getOutboxEventId(), OutboxEventStatus.FAILED))
                .thenReturn(Optional.of(event));

        OutboxEventResponse response = service.retryFailedEvent(event.getOutboxEventId(), actorUserId);

        assertThat(response.outboxEventId()).isEqualTo(event.getOutboxEventId());
        assertThat(response.status()).isEqualTo(OutboxEventStatus.PENDING);
        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
        assertThat(event.getProcessedAt()).isNull();
        assertThat(event.getNextAttemptAt()).isBeforeOrEqualTo(LocalDateTime.now());
        verify(auditService).recordRetryRequested(actorUserId, event);
    }

    @Test
    void retryFailedEventRejectsNonFailedEvent() {
        OutboxEventRepository repository = mock(OutboxEventRepository.class);
        OutboxEventAuditService auditService = mock(OutboxEventAuditService.class);
        OutboxEventRetryService service = new OutboxEventRetryService(repository, auditService);
        OutboxEvent event = pendingEvent();
        when(repository.findByOutboxEventIdAndStatus(event.getOutboxEventId(), OutboxEventStatus.FAILED))
                .thenReturn(Optional.empty());
        when(repository.existsById(event.getOutboxEventId())).thenReturn(true);

        assertThatThrownBy(() -> service.retryFailedEvent(event.getOutboxEventId(), UUID.randomUUID()))
                .isInstanceOf(BusinessException.class);

        verify(auditService, never()).recordRetryRequested(any(), any());
    }

    private OutboxEvent failedEvent() {
        OutboxEvent event = OutboxEvent.builder()
                .eventType(OutboxEventType.FEEDBACK_MATTERMOST_NOTIFICATION)
                .aggregateType("FEEDBACK")
                .aggregateId(UUID.randomUUID())
                .payload(Map.of("feedbackId", UUID.randomUUID().toString()))
                .maxRetryCount(1)
                .build();
        event.markProcessing();
        event.recordFailure("webhook timeout", LocalDateTime.now().plusMinutes(1));
        return event;
    }

    private OutboxEvent pendingEvent() {
        return OutboxEvent.builder()
                .eventType(OutboxEventType.FEEDBACK_MATTERMOST_NOTIFICATION)
                .aggregateType("FEEDBACK")
                .aggregateId(UUID.randomUUID())
                .payload(Map.of("feedbackId", UUID.randomUUID().toString()))
                .build();
    }
}
