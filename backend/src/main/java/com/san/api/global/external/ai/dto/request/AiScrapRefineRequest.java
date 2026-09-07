package com.san.api.global.external.ai.dto.request;

import java.util.List;

/** AI 원본 정제 요청 DTO */
public record AiScrapRefineRequest(
        String inputType,
        String content
) {

    public AiTilRequest toTilRequest() {
        return new AiTilRequest(
                List.of(new AiTilContentRequest(inputType, content)),
                true
        );
    }
}
