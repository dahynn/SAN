package com.san.api.global.outbox.dto.request;

import com.san.api.global.outbox.entity.OutboxEventStatus;
import com.san.api.global.outbox.entity.OutboxEventType;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.UUID;

/** Outbox 이벤트 운영 조회 조건입니다. */
public record OutboxEventSearchRequest(
        OutboxEventType eventType,
        OutboxEventStatus status,
        String aggregateType,
        UUID aggregateId,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        LocalDateTime createdFrom,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        LocalDateTime createdTo
) {
}
