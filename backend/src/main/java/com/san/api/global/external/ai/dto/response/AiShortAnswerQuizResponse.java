package com.san.api.global.external.ai.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** AI 단답형 리콜 퀴즈 생성 응답 DTO */
public record AiShortAnswerQuizResponse(
        @JsonProperty("quiz_type")
        String quizType,

        List<AiShortAnswerQuizQuestionResponse> questions
) {
}
