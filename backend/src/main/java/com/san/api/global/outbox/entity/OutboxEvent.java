package com.san.api.global.outbox.entity;

import com.san.api.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * 외부 알림/연동 이벤트를 유실 없이 처리하기 위해 저장하는 Outbox 이벤트 엔티티
 */
@Getter
@Entity
@Table(
        name = "outbox_events",
        indexes = {
                @Index(name = "idx_outbox_events_status_next_attempt", columnList = "status, next_attempt_at, created_at"),
                @Index(name = "idx_outbox_events_event_type_created_at", columnList = "event_type, created_at"),
                @Index(name = "idx_outbox_events_aggregate", columnList = "aggregate_type, aggregate_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboxEvent extends BaseEntity {

    @Id
    @Column(name = "outbox_event_id", columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID outboxEventId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 100)
    private OutboxEventType eventType;

    @Column(name = "aggregate_type", nullable = false, length = 50)
    private String aggregateType;

    @Column(name = "aggregate_id", columnDefinition = "uuid")
    private UUID aggregateId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OutboxEventStatus status;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "max_retry_count", nullable = false)
    private int maxRetryCount;

    @Column(name = "next_attempt_at", nullable = false)
    private LocalDateTime nextAttemptAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "last_error_message", columnDefinition = "text")
    private String lastErrorMessage;

    /**
     * 처리 대기 상태의 Outbox 이벤트를 생성합니다.
     *
     * @param eventType     처리할 이벤트 유형
     * @param aggregateType 이벤트가 발생한 원본 도메인 유형
     * @param aggregateId   이벤트가 발생한 원본 도메인 식별자
     * @param payload       외부 전송에 필요한 데이터 스냅샷
     * @param maxRetryCount 최대 재시도 횟수
     * @param nextAttemptAt 다음 처리 가능 시각
     */
    @Builder
    private OutboxEvent(
            OutboxEventType eventType,
            String aggregateType,
            UUID aggregateId,
            Map<String, Object> payload,
            Integer maxRetryCount,
            LocalDateTime nextAttemptAt
    ) {
        this.outboxEventId = UUID.randomUUID();
        this.eventType = Objects.requireNonNull(eventType, "eventType must not be null");
        this.aggregateType = requireText(aggregateType, "aggregateType");
        this.aggregateId = aggregateId;
        this.payload = new LinkedHashMap<>(Objects.requireNonNull(payload, "payload must not be null"));
        this.status = OutboxEventStatus.PENDING;
        this.retryCount = 0;
        this.maxRetryCount = maxRetryCount == null ? 3 : Math.max(1, maxRetryCount);
        this.nextAttemptAt = nextAttemptAt == null ? LocalDateTime.now() : nextAttemptAt;
    }

    /** 이벤트 처리를 시작했음을 표시합니다. */
    public void markProcessing() {
        this.status = OutboxEventStatus.PROCESSING;
    }

    /** 이벤트 처리가 성공적으로 끝났음을 표시합니다. */
    public void markSent() {
        this.status = OutboxEventStatus.SENT;
        this.processedAt = LocalDateTime.now();
        this.lastErrorMessage = null;
    }

    /**
     * 이벤트 처리 실패를 기록하고 재시도 가능 여부에 따라 상태를 변경합니다.
     *
     * @param errorMessage  마지막 실패 사유
     * @param nextAttemptAt 다음 재시도 가능 시각
     */
    public void recordFailure(String errorMessage, LocalDateTime nextAttemptAt) {
        this.retryCount++;
        this.lastErrorMessage = errorMessage;
        this.nextAttemptAt = nextAttemptAt == null ? LocalDateTime.now() : nextAttemptAt;
        this.status = retryCount >= maxRetryCount ? OutboxEventStatus.FAILED : OutboxEventStatus.PENDING;
    }

    /**
     * 실패했거나 처리 중이던 이벤트를 다시 처리 대기 상태로 되돌립니다.
     *
     * @param nextAttemptAt 다음 처리 가능 시각
     */
    public void resetForRetry(LocalDateTime nextAttemptAt) {
        this.status = OutboxEventStatus.PENDING;
        this.nextAttemptAt = nextAttemptAt == null ? LocalDateTime.now() : nextAttemptAt;
        this.processedAt = null;
    }

    private String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
