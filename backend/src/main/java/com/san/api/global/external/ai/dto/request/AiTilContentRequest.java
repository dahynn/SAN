package com.san.api.global.external.ai.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

/** AI TIL 생성 원본 단건 요청 DTO */
public record AiTilContentRequest(
        @JsonProperty("input_type")
        String inputType,
        String content
) {
}
