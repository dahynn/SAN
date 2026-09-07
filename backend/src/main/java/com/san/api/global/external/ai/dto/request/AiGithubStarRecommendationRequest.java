package com.san.api.global.external.ai.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

/** GitHub Star 추천 요청 DTO */
public record AiGithubStarRecommendationRequest(
        @JsonProperty("github_username")
        String githubUsername,
        Integer limit
) {
}
