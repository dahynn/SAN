package com.san.api.domain.til.dto.response;

import java.time.LocalDate;
import java.util.List;

/** TIL GitHub contribution 잔디 조회 응답 DTO */
public record TilGithubContributionResponse(
        LocalDate from,
        LocalDate to,
        int totalCommits,
        int activeDays,
        int currentStreakDays,
        int longestStreakDays,
        List<TilGithubContributionDayResponse> days,
        List<TilGithubContributionRepositoryResponse> repositories,
        List<TilGithubContributionCommitResponse> commits
) {
}
