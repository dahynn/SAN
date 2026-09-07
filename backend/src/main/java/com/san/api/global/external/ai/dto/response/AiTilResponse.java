package com.san.api.global.external.ai.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

/** AI TIL 생성 응답 DTO */
public record AiTilResponse(
        String title,

        @JsonProperty("til_markdown")
        String tilMarkdown,

        float[] embedding
) {
}
