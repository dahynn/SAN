package com.san.api.global.external.ai.dto.response;

/** AI 단답형 리콜 퀴즈 문제 응답 DTO */
public record AiShortAnswerQuizQuestionResponse(
        String question,

        String answer,

        String explanation
) {
}
