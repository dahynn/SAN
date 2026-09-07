package com.san.api.global.external.ai.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** AI 리콜 퀴즈 생성 요청 DTO */
public record AiQuizRequest(
        List<AiQuizContentRequest> contents,

        @JsonProperty("quiz_type")
        String quizType
) {
}
