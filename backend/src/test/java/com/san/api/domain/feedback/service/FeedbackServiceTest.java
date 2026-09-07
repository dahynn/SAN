package com.san.api.domain.feedback.service;

import com.san.api.domain.auth.entity.ClientType;
import com.san.api.domain.feedback.dto.request.FeedbackCreateRequest;
import com.san.api.domain.feedback.entity.Feedback;
import com.san.api.domain.feedback.entity.FeedbackStatus;
import com.san.api.domain.feedback.entity.FeedbackType;
import com.san.api.domain.feedback.repository.FeedbackRepository;
import com.san.api.domain.user.entity.User;
import com.san.api.global.audit.context.AuditRequestContext;
import com.san.api.global.audit.context.AuditRequestContextHolder;
import com.san.api.global.outbox.entity.OutboxEventType;
import com.san.api.global.outbox.service.OutboxEventAppenderService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeedbackServiceTest {

    @Mock
    private FeedbackRepository feedbackRepository;

    @Mock
    private OutboxEventAppenderService outboxEventAppenderService;

    @Mock
    private EntityManager entityManager;

    private FeedbackService feedbackService;

    @BeforeEach
    void setUp() {
        feedbackService = new FeedbackService(feedbackRepository, outboxEventAppenderService, entityManager);
    }

    @AfterEach
    void tearDown() {
        AuditRequestContextHolder.clear();
    }

    @Test
    @SuppressWarnings("unchecked")
    void createFeedbackSavesFeedbackWithRequestMetadataAndAppendsOutboxEvent() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .username("dahyeon")
                .passwordHash("password")
                .build();
        FeedbackCreateRequest request = new FeedbackCreateRequest(
                FeedbackType.BUG,
                "save button freezes",
                "user@example.com",
                "https://san.example/cards"
        );
        AuditRequestContextHolder.set(new AuditRequestContext(
                "trace-1",
                "203.0.113.10",
                "Mozilla/5.0"
        ));
        when(entityManager.getReference(User.class, userId)).thenReturn(user);
        when(feedbackRepository.save(org.mockito.ArgumentMatchers.any(Feedback.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UUID feedbackId = feedbackService.createFeedback(userId, ClientType.DASHBOARD, request);

        ArgumentCaptor<Feedback> feedbackCaptor = ArgumentCaptor.forClass(Feedback.class);
        verify(feedbackRepository).save(feedbackCaptor.capture());
        Feedback saved = feedbackCaptor.getValue();
        assertThat(feedbackId).isEqualTo(saved.getFeedbackId());
        assertThat(saved.getUser()).isEqualTo(user);
        assertThat(saved.getType()).isEqualTo(FeedbackType.BUG);
        assertThat(saved.getContent()).isEqualTo("save button freezes");
        assertThat(saved.getContact()).isEqualTo("user@example.com");
        assertThat(saved.getPageUrl()).isEqualTo("https://san.example/cards");
        assertThat(saved.getClientType()).isEqualTo(ClientType.DASHBOARD);
        assertThat(saved.getTraceId()).isEqualTo("trace-1");
        assertThat(saved.getIpAddress()).isEqualTo("203.0.113.10");
        assertThat(saved.getUserAgent()).isEqualTo("Mozilla/5.0");
        assertThat(saved.getStatus()).isEqualTo(FeedbackStatus.NEW);

        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(outboxEventAppenderService).append(
                eq(OutboxEventType.FEEDBACK_MATTERMOST_NOTIFICATION),
                eq("FEEDBACK"),
                eq(saved.getFeedbackId()),
                payloadCaptor.capture()
        );
        assertThat(payloadCaptor.getValue())
                .containsEntry("feedbackId", saved.getFeedbackId().toString())
                .containsEntry("type", FeedbackType.BUG.name())
                .containsEntry("userId", userId.toString())
                .containsEntry("clientType", ClientType.DASHBOARD.name())
                .containsEntry("pageUrl", "https://san.example/cards")
                .containsEntry("traceId", "trace-1")
                .containsEntry("contact", "user@example.com")
                .containsEntry("content", "save button freezes");
    }
}
