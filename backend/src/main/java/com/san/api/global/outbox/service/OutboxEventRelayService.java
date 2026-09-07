package com.san.api.global.outbox.service;

import com.san.api.domain.auth.entity.ClientType;
import com.san.api.domain.feedback.entity.FeedbackType;
import com.san.api.domain.feedback.service.FeedbackNotificationPayload;
import com.san.api.domain.feedback.service.MattermostFeedbackNotifier;
import com.san.api.global.outbox.entity.OutboxEvent;
import com.san.api.global.outbox.entity.OutboxEventStatus;
import com.san.api.global.outbox.entity.OutboxEventType;
import com.san.api.global.outbox.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** 처리 대기 중인 Outbox 이벤트를 외부 시스템으로 전달하고 처리 상태를 갱신합니다. */
@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxEventRelayService {

    private static final Duration PROCESSING_STALE_THRESHOLD = Duration.ofMinutes(10);
    private static final List<Duration> RETRY_DELAYS = List.of(
            Duration.ofMinutes(1),
            Duration.ofMinutes(5),
            Duration.ofMinutes(15)
    );

    private final OutboxEventRepository outboxEventRepository;
    private final MattermostFeedbackNotifier mattermostFeedbackNotifier;
    private final TransactionTemplate transactionTemplate;

    /** 처리 가능한 Outbox 이벤트를 조회해 순차적으로 전달합니다. */
    public void relayDueEvents() {
        LocalDateTime now = LocalDateTime.now();
        recoverStaleProcessingEvents(now);
        List<UUID> eventIds = outboxEventRepository
                .findTop100ByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                        OutboxEventStatus.PENDING,
                        now
                )
                .stream()
                .map(OutboxEvent::getOutboxEventId)
                .toList();

        eventIds.forEach(this::relay);
    }

    private void recoverStaleProcessingEvents(LocalDateTime now) {
        LocalDateTime threshold = now.minus(PROCESSING_STALE_THRESHOLD);
        transactionTemplate.executeWithoutResult(status -> {
            List<OutboxEvent> staleEvents = outboxEventRepository.findByStatusAndUpdatedAtBefore(
                    OutboxEventStatus.PROCESSING,
                    threshold
            );
            staleEvents.forEach(event -> event.recordFailure(
                    "Stale PROCESSING event recovered by outbox relay",
                    now.plus(calculateRetryDelay(event.getRetryCount() + 1))
            ));

            if (!staleEvents.isEmpty()) {
                log.warn("[OutboxRelay] stale processing events recovered. count={}", staleEvents.size());
            }
        });
    }

    private void relay(UUID outboxEventId) {
        OutboxEvent event = markProcessing(outboxEventId);
        if (event == null) {
            return;
        }

        try {
            dispatch(event);
            markSent(outboxEventId);
        } catch (Exception e) {
            recordFailure(outboxEventId, e);
        }
    }

    private OutboxEvent markProcessing(UUID outboxEventId) {
        return transactionTemplate.execute(status -> outboxEventRepository
                .findByOutboxEventIdAndStatus(outboxEventId, OutboxEventStatus.PENDING)
                .map(event -> {
                    event.markProcessing();
                    return event;
                })
                .orElse(null));
    }

    private void dispatch(OutboxEvent event) {
        if (event.getEventType() == OutboxEventType.FEEDBACK_MATTERMOST_NOTIFICATION) {
            mattermostFeedbackNotifier.send(toFeedbackNotificationPayload(event.getPayload()));
            return;
        }

        throw new IllegalArgumentException("Unsupported outbox event type: " + event.getEventType());
    }

    private void markSent(UUID outboxEventId) {
        transactionTemplate.executeWithoutResult(status -> outboxEventRepository.findById(outboxEventId)
                .ifPresent(OutboxEvent::markSent));
    }

    private void recordFailure(UUID outboxEventId, Exception exception) {
        transactionTemplate.executeWithoutResult(status -> outboxEventRepository.findById(outboxEventId)
                .ifPresent(event -> event.recordFailure(
                        resolveErrorMessage(exception),
                        LocalDateTime.now().plus(calculateRetryDelay(event.getRetryCount() + 1))
                )));
        log.warn("[OutboxRelay] event processing failed. outboxEventId={}", outboxEventId, exception);
    }

    private Duration calculateRetryDelay(int nextRetryCount) {
        int index = Math.max(0, Math.min(nextRetryCount - 1, RETRY_DELAYS.size() - 1));
        return RETRY_DELAYS.get(index);
    }

    private FeedbackNotificationPayload toFeedbackNotificationPayload(Map<String, Object> payload) {
        return new FeedbackNotificationPayload(
                UUID.fromString(required(payload, "feedbackId")),
                FeedbackType.valueOf(required(payload, "type")),
                UUID.fromString(required(payload, "userId")),
                optionalEnum(payload, "clientType", ClientType.class),
                optional(payload, "pageUrl"),
                optional(payload, "traceId"),
                optional(payload, "contact"),
                required(payload, "content")
        );
    }

    private String resolveErrorMessage(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }
        return exception.getClass().getSimpleName() + ": " + message;
    }

    private String required(Map<String, Object> payload, String key) {
        String value = optional(payload, key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Outbox payload missing required field: " + key);
        }
        return value;
    }

    private String optional(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        return value == null ? null : value.toString();
    }

    private <T extends Enum<T>> T optionalEnum(Map<String, Object> payload, String key, Class<T> enumType) {
        String value = optional(payload, key);
        if (value == null || value.isBlank()) {
            return null;
        }
        return Enum.valueOf(enumType, value);
    }
}
