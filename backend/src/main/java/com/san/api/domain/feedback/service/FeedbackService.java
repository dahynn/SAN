package com.san.api.domain.feedback.service;

import com.san.api.domain.auth.entity.ClientType;
import com.san.api.domain.feedback.dto.request.FeedbackCreateRequest;
import com.san.api.domain.feedback.entity.Feedback;
import com.san.api.domain.feedback.repository.FeedbackRepository;
import com.san.api.domain.user.entity.User;
import com.san.api.global.audit.context.AuditRequestContext;
import com.san.api.global.audit.context.AuditRequestContextHolder;
import com.san.api.global.outbox.entity.OutboxEventType;
import com.san.api.global.outbox.service.OutboxEventAppenderService;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** 서비스 피드백 저장과 피드백 알림 Outbox 이벤트 생성을 담당합니다. */
@Service
@RequiredArgsConstructor
public class FeedbackService {

    private static final String FEEDBACK_AGGREGATE_TYPE = "FEEDBACK";

    private final FeedbackRepository feedbackRepository;
    private final OutboxEventAppenderService outboxEventAppenderService;
    private final EntityManager entityManager;

    /**
     * 사용자 피드백을 저장하고 같은 트랜잭션 안에서 Mattermost 알림 Outbox 이벤트를 기록합니다.
     *
     * @param userId     피드백을 등록한 사용자 ID
     * @param clientType 피드백을 보낸 클라이언트 유형
     * @param request    피드백 생성 요청
     * @return 생성된 피드백 ID
     */
    @Transactional
    public UUID createFeedback(UUID userId, ClientType clientType, FeedbackCreateRequest request) {
        AuditRequestContext context = AuditRequestContextHolder.get().orElse(null);
        Feedback feedback = Feedback.builder()
                .user(entityManager.getReference(User.class, userId))
                .type(request.type())
                .content(request.content())
                .contact(blankToNull(request.contact()))
                .pageUrl(blankToNull(request.pageUrl()))
                .clientType(clientType)
                .traceId(context == null ? null : context.traceId())
                .ipAddress(context == null ? null : context.ipAddress())
                .userAgent(context == null ? null : context.userAgent())
                .build();

        Feedback saved = feedbackRepository.save(feedback);
        outboxEventAppenderService.append(
                OutboxEventType.FEEDBACK_MATTERMOST_NOTIFICATION,
                FEEDBACK_AGGREGATE_TYPE,
                saved.getFeedbackId(),
                createOutboxPayload(saved, userId)
        );
        return saved.getFeedbackId();
    }

    /** 원본 피드백 변경과 무관하게 재처리할 수 있도록 알림에 필요한 값을 스냅샷으로 복사합니다. */
    private Map<String, Object> createOutboxPayload(Feedback feedback, UUID userId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("feedbackId", feedback.getFeedbackId().toString());
        payload.put("type", feedback.getType().name());
        payload.put("userId", userId.toString());
        payload.put("clientType", feedback.getClientType() == null ? null : feedback.getClientType().name());
        payload.put("pageUrl", feedback.getPageUrl());
        payload.put("traceId", feedback.getTraceId());
        payload.put("contact", feedback.getContact());
        payload.put("content", feedback.getContent());
        return payload;
    }

    /** 빈 문자열은 DB에 null로 저장합니다. */
    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }
}
