package com.san.api.global.outbox.service;

import com.san.api.domain.auth.entity.ClientType;
import com.san.api.domain.feedback.entity.FeedbackType;
import com.san.api.domain.feedback.service.FeedbackNotificationPayload;
import com.san.api.domain.feedback.service.MattermostFeedbackNotifier;
import com.san.api.global.outbox.entity.OutboxEvent;
import com.san.api.global.outbox.entity.OutboxEventStatus;
import com.san.api.global.outbox.entity.OutboxEventType;
import com.san.api.global.outbox.repository.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClientException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class OutboxEventRelayServiceTest {

    private OutboxEventRepository outboxEventRepository;
    private MattermostFeedbackNotifier mattermostFeedbackNotifier;
    private OutboxEventRelayService outboxEventRelayService;

    @BeforeEach
    void setUp() {
        outboxEventRepository = mock(OutboxEventRepository.class);
        mattermostFeedbackNotifier = mock(MattermostFeedbackNotifier.class);
        TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);

        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(new SimpleTransactionStatus());
        });
        doAnswer(invocation -> {
            Consumer<TransactionStatus> callback = invocation.getArgument(0);
            callback.accept(new SimpleTransactionStatus());
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());

        outboxEventRelayService = new OutboxEventRelayService(
                outboxEventRepository,
                mattermostFeedbackNotifier,
                transactionTemplate
        );
    }

    @Test
    void relayDueEventsSendsMattermostNotificationAndMarksSent() {
        UUID feedbackId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        OutboxEvent event = feedbackNotificationEvent(feedbackId, userId);
        when(outboxEventRepository.findTop100ByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                eq(OutboxEventStatus.PENDING),
                any(LocalDateTime.class)
        )).thenReturn(List.of(event));
        when(outboxEventRepository.findByStatusAndUpdatedAtBefore(
                eq(OutboxEventStatus.PROCESSING),
                any(LocalDateTime.class)
        )).thenReturn(List.of());
        when(outboxEventRepository.findByOutboxEventIdAndStatus(
                event.getOutboxEventId(),
                OutboxEventStatus.PENDING
        )).thenReturn(Optional.of(event));
        when(outboxEventRepository.findById(event.getOutboxEventId())).thenReturn(Optional.of(event));

        outboxEventRelayService.relayDueEvents();

        ArgumentCaptor<FeedbackNotificationPayload> payloadCaptor =
                ArgumentCaptor.forClass(FeedbackNotificationPayload.class);
        verify(mattermostFeedbackNotifier).send(payloadCaptor.capture());
        FeedbackNotificationPayload payload = payloadCaptor.getValue();
        assertThat(payload.feedbackId()).isEqualTo(feedbackId);
        assertThat(payload.type()).isEqualTo(FeedbackType.BUG);
        assertThat(payload.userId()).isEqualTo(userId);
        assertThat(payload.clientType()).isEqualTo(ClientType.DASHBOARD);
        assertThat(payload.content()).isEqualTo("save button freezes");
        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.SENT);
        assertThat(event.getProcessedAt()).isNotNull();
    }

    @Test
    void relayDueEventsRecordsFailureWhenMattermostFails() {
        OutboxEvent event = feedbackNotificationEvent(UUID.randomUUID(), UUID.randomUUID());
        when(outboxEventRepository.findTop100ByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                eq(OutboxEventStatus.PENDING),
                any(LocalDateTime.class)
        )).thenReturn(List.of(event));
        when(outboxEventRepository.findByStatusAndUpdatedAtBefore(
                eq(OutboxEventStatus.PROCESSING),
                any(LocalDateTime.class)
        )).thenReturn(List.of());
        when(outboxEventRepository.findByOutboxEventIdAndStatus(
                event.getOutboxEventId(),
                OutboxEventStatus.PENDING
        )).thenReturn(Optional.of(event));
        when(outboxEventRepository.findById(event.getOutboxEventId())).thenReturn(Optional.of(event));
        doThrow(new RestClientException("webhook timeout"))
                .when(mattermostFeedbackNotifier)
                .send(any(FeedbackNotificationPayload.class));

        outboxEventRelayService.relayDueEvents();

        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
        assertThat(event.getRetryCount()).isEqualTo(1);
        assertThat(event.getLastErrorMessage()).contains("webhook timeout");
        assertThat(event.getNextAttemptAt()).isAfter(LocalDateTime.now());
    }

    @Test
    void relayDueEventsRecoversStaleProcessingEventsBeforePollingPendingEvents() {
        OutboxEvent stale = feedbackNotificationEvent(UUID.randomUUID(), UUID.randomUUID());
        stale.markProcessing();
        when(outboxEventRepository.findByStatusAndUpdatedAtBefore(
                eq(OutboxEventStatus.PROCESSING),
                any(LocalDateTime.class)
        )).thenReturn(List.of(stale));
        when(outboxEventRepository.findTop100ByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                eq(OutboxEventStatus.PENDING),
                any(LocalDateTime.class)
        )).thenReturn(List.of());

        outboxEventRelayService.relayDueEvents();

        assertThat(stale.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
        assertThat(stale.getRetryCount()).isEqualTo(1);
        assertThat(stale.getLastErrorMessage()).contains("Stale PROCESSING event recovered");
    }

    private OutboxEvent feedbackNotificationEvent(UUID feedbackId, UUID userId) {
        return OutboxEvent.builder()
                .eventType(OutboxEventType.FEEDBACK_MATTERMOST_NOTIFICATION)
                .aggregateType("FEEDBACK")
                .aggregateId(feedbackId)
                .payload(Map.of(
                        "feedbackId", feedbackId.toString(),
                        "type", FeedbackType.BUG.name(),
                        "userId", userId.toString(),
                        "clientType", ClientType.DASHBOARD.name(),
                        "pageUrl", "https://san.example/cards",
                        "traceId", "trace-1",
                        "contact", "user@example.com",
                        "content", "save button freezes"
                ))
                .build();
    }
}
