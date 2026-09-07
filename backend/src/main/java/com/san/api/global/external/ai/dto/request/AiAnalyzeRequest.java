package com.san.api.global.external.ai.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

/** AI 분석 요청 DTO */
public record AiAnalyzeRequest(
        @JsonProperty("input_type")
        String inputType,       // 분석 요청 데이터 source type
        String content
) {
}
