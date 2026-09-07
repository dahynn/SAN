package com.san.api.domain.feedback.dto.request;

import com.san.api.domain.feedback.entity.FeedbackType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 서비스 피드백 등록 요청 DTO. */
public record FeedbackCreateRequest(
        /** 피드백 유형. */
        @NotNull
        FeedbackType type,

        /** 사용자가 작성한 피드백 본문. */
        @NotBlank
        @Size(max = 5000)
        String content,

        /** 답변이 필요한 경우 사용자가 남긴 연락처. */
        @Size(max = 255)
        String contact,

        /** 피드백이 발생한 프론트엔드 화면 URL. */
        @Size(max = 1000)
        String pageUrl
) {
}
