package com.san.api.global.external.ai.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

/** AI OX 리콜 퀴즈 문제 응답 DTO */
public record AiOxQuizQuestionResponse(
        String statement,

        @JsonProperty("is_correct")
        Boolean isCorrect,

        String explanation
) {
}
