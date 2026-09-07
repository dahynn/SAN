package com.san.api.global.outbox.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxEventTest {

    @Test
    void createsPendingEventWithPayloadSnapshot() {
        UUID aggregateId = UUID.randomUUID();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("feedbackId", aggregateId.toString());
        payload.put("contact", null);

        OutboxEvent event = OutboxEvent.builder()
                .eventType(OutboxEventType.FEEDBACK_MATTERMOST_NOTIFICATION)
                .aggregateType("FEEDBACK")
                .aggregateId(aggregateId)
                .payload(payload)
                .build();

        payload.put("contact", "changed@example.com");

        assertThat(event.getOutboxEventId()).isNotNull();
        assertThat(event.getEventType()).isEqualTo(OutboxEventType.FEEDBACK_MATTERMOST_NOTIFICATION);
        assertThat(event.getAggregateType()).isEqualTo("FEEDBACK");
        assertThat(event.getAggregateId()).isEqualTo(aggregateId);
        assertThat(event.getPayload()).containsEntry("contact", null);
        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
        assertThat(event.getRetryCount()).isZero();
        assertThat(event.getMaxRetryCount()).isEqualTo(3);
        assertThat(event.getNextAttemptAt()).isNotNull();
    }

    @Test
    void recordsFailureAndMovesToFailedAfterMaxRetryCount() {
        LocalDateTime nextAttemptAt = LocalDateTime.of(2026, 5, 19, 12, 30);
        OutboxEvent event = OutboxEvent.builder()
                .eventType(OutboxEventType.FEEDBACK_MATTERMOST_NOTIFICATION)
                .aggregateType("FEEDBACK")
                .payload(Map.of("feedbackId", UUID.randomUUID().toString()))
                .maxRetryCount(2)
                .build();

        event.markProcessing();
        event.recordFailure("webhook timeout", nextAttemptAt);

        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
        assertThat(event.getRetryCount()).isEqualTo(1);
        assertThat(event.getLastErrorMessage()).isEqualTo("webhook timeout");
        assertThat(event.getNextAttemptAt()).isEqualTo(nextAttemptAt);

        event.markProcessing();
        event.recordFailure("webhook timeout", nextAttemptAt.plusMinutes(1));

        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.FAILED);
        assertThat(event.getRetryCount()).isEqualTo(2);
    }
}
