package com.san.api.global.external.ai.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** AI TIL 생성 요청 DTO */
public record AiTilRequest(
        List<AiTilContentRequest> contents,

        @JsonProperty("generate_til")
        boolean generateTil
) {
}
