package com.san.api.domain.github.dto.response;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.san.api.domain.github.entity.GithubStarRecommendation;

import java.util.List;

/** GitHub Star 추천 후보 목록 응답 DTO */
public record GithubStarRecommendationListResponse(
        List<GithubStarRecommendationResponse> recommendations
) {

    public static GithubStarRecommendationListResponse from(
            List<GithubStarRecommendation> recommendations,
            ObjectMapper objectMapper
    ) {
        return new GithubStarRecommendationListResponse(
                recommendations.stream()
                        .map(recommendation -> GithubStarRecommendationResponse.from(recommendation, objectMapper))
                        .toList()
        );
    }
}
