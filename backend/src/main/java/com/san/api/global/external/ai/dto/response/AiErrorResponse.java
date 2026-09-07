package com.san.api.global.external.ai.dto.response;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

/** AI 에러 응답 DTO */
public record AiErrorResponse(
        @JsonProperty("error_code")
        @JsonAlias("code")
        String errorCode,
        String message
) {
}
