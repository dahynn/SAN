package com.san.api.domain.github.dto.response;

import java.util.UUID;

/** GitHub Star 추천 후보 수집 응답 DTO */
public record GithubStarRecommendationCollectResponse(
        UUID recommendationId,
        UUID scrapId,
        UUID cardId,
        boolean collected
) {
}
