package com.san.api.domain.til.dto.response;

/** TIL GitHub contribution 레포지토리별 집계 응답 DTO */
public record TilGithubContributionRepositoryResponse(
        Long githubRepositoryId,
        String name,
        String fullName,
        String htmlUrl,
        int count
) {
}
