package com.san.api.domain.feedback.dto.response;

import java.util.UUID;

/** 서비스 피드백 등록 응답 DTO. */
public record FeedbackCreateResponse(
        /** 등록된 피드백 ID. */
        UUID feedbackId
) {
}
