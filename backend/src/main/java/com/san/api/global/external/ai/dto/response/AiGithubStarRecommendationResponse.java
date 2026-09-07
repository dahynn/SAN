package com.san.api.global.external.ai.dto.response;

import com.san.api.global.external.ai.dto.request.AiAnalyzeRequest;

import java.util.List;

/** GitHub Star 추천 응답 DTO */
public record AiGithubStarRecommendationResponse(
        List<AiAnalyzeRequest> recommendations
) {
}
