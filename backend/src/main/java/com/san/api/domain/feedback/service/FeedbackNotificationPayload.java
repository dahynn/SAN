package com.san.api.domain.feedback.service;

import com.san.api.domain.auth.entity.ClientType;
import com.san.api.domain.feedback.entity.FeedbackType;

import java.util.UUID;

/** Mattermost 피드백 알림에 필요한 값만 담은 payload. */
public record FeedbackNotificationPayload(
        UUID feedbackId,
        FeedbackType type,
        UUID userId,
        ClientType clientType,
        String pageUrl,
        String traceId,
        String contact,
        String content
) {
}
