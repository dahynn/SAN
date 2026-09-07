package com.san.api.global.external.ai.dto.response;

/** AI 원본 정제 응답 DTO */
public record AiScrapRefineResponse(
        String refinedContent
) {

    public static AiScrapRefineResponse from(AiTilResponse response) {
        return new AiScrapRefineResponse(response.tilMarkdown());
    }
}
