package com.san.api.domain.recall.dto.request;

import jakarta.validation.constraints.NotBlank;

/** 리콜 퀴즈 정답 제출 요청 DTO */
public record RecallQuizSubmitRequest(
        @NotBlank(message = "제출 답변을 입력해주세요.")
        String answer
) {
}
