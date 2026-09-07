package com.san.api.global.outbox.service;

import com.san.api.global.outbox.entity.OutboxEvent;
import com.san.api.global.outbox.entity.OutboxEventStatus;
import com.san.api.global.outbox.entity.OutboxEventType;
import com.san.api.global.outbox.repository.OutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class OutboxEventAppenderServiceTest {

    @Test
    void appendsOutboxEvent() {
        OutboxEventRepository repository = mock(OutboxEventRepository.class);
        OutboxEventAppenderService appender = new OutboxEventAppenderService(repository);
        UUID aggregateId = UUID.randomUUID();
        Map<String, Object> payload = Map.of("feedbackId", aggregateId.toString());
        when(repository.save(org.mockito.ArgumentMatchers.any(OutboxEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OutboxEvent event = appender.append(
                OutboxEventType.FEEDBACK_MATTERMOST_NOTIFICATION,
                "FEEDBACK",
                aggregateId,
                payload
        );

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(repository).save(captor.capture());
        assertThat(event).isEqualTo(captor.getValue());
        assertThat(event.getEventType()).isEqualTo(OutboxEventType.FEEDBACK_MATTERMOST_NOTIFICATION);
        assertThat(event.getAggregateType()).isEqualTo("FEEDBACK");
        assertThat(event.getAggregateId()).isEqualTo(aggregateId);
        assertThat(event.getPayload()).containsEntry("feedbackId", aggregateId.toString());
        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
        assertThat(event.getMaxRetryCount()).isEqualTo(3);
    }
}
