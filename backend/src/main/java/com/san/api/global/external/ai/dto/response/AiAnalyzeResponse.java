package com.san.api.global.external.ai.dto.response;

import java.util.List;

/** AI 분석 응답 DTO */
public record AiAnalyzeResponse(
        String title,
        String summary,
        List<String> tags,
        String category,
        float[] embedding
) {
}
