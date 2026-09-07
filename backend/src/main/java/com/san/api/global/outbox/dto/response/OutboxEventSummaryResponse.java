package com.san.api.global.outbox.dto.response;

import com.san.api.global.outbox.entity.OutboxEvent;
import com.san.api.global.outbox.entity.OutboxEventStatus;
import com.san.api.global.outbox.entity.OutboxEventType;

import java.time.LocalDateTime;
import java.util.UUID;

/** 운영 목록 조회에 노출할 Outbox 이벤트 요약 응답입니다. */
public record OutboxEventSummaryResponse(
        UUID outboxEventId,
        OutboxEventType eventType,
        String eventTypeDescription,
        String aggregateType,
        UUID aggregateId,
        OutboxEventStatus status,
        String statusDescription,
        int retryCount,
        int maxRetryCount,
        LocalDateTime nextAttemptAt,
        LocalDateTime processedAt,
        String lastErrorMessage,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static OutboxEventSummaryResponse from(OutboxEvent event) {
        return new OutboxEventSummaryResponse(
                event.getOutboxEventId(),
                event.getEventType(),
                event.getEventType().getDescription(),
                event.getAggregateType(),
                event.getAggregateId(),
                event.getStatus(),
                event.getStatus().getDescription(),
                event.getRetryCount(),
                event.getMaxRetryCount(),
                event.getNextAttemptAt(),
                event.getProcessedAt(),
                event.getLastErrorMessage(),
                event.getCreatedAt(),
                event.getUpdatedAt()
        );
    }
}
