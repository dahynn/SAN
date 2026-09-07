package com.san.api.global.external.ai.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

/** AI 리콜 퀴즈 생성 콘텐츠 요청 DTO */
public record AiQuizContentRequest(
        @JsonProperty("input_type")
        String inputType,

        String content
) {
}
