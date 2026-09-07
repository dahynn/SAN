package com.san.api.domain.github.dto.response;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.san.api.domain.github.entity.GithubStarRecommendation;

import java.util.List;
import java.util.UUID;

/** GitHub Star 추천 작업 응답 DTO */
public record GithubStarRecommendationJobResponse(
        UUID jobId,
        boolean alreadyRecommended,
        List<GithubStarRecommendationResponse> recommendations
) {

    public static GithubStarRecommendationJobResponse created(UUID jobId) {
        return new GithubStarRecommendationJobResponse(jobId, false, List.of());
    }

    public static GithubStarRecommendationJobResponse alreadyRecommended(
            List<GithubStarRecommendation> recommendations,
            ObjectMapper objectMapper
    ) {
        return new GithubStarRecommendationJobResponse(
                null,
                true,
                recommendations.stream()
                        .map(recommendation -> GithubStarRecommendationResponse.from(recommendation, objectMapper))
                        .toList()
        );
    }
}
