package com.san.api.domain.til.dto.response;

import com.san.api.domain.til.entity.TilGithubCommit;

import java.time.LocalDateTime;
import java.util.UUID;

/** TIL GitHub contribution 잔디에서 날짜 선택 시 표시할 커밋 상세 응답 DTO */
public record TilGithubContributionCommitResponse(
        UUID commitId,
        UUID summaryId,
        Long githubRepositoryId,
        String repositoryName,
        String repositoryFullName,
        String branch,
        String filePath,
        String title,
        String commitSha,
        String commitUrl,
        LocalDateTime pushedAt
) {

    /**
     * TIL GitHub 커밋 이력을 contribution 커밋 상세 응답으로 변환합니다.
     *
     * @param commit TIL GitHub 커밋 이력
     * @return contribution 커밋 상세 응답
     */
    public static TilGithubContributionCommitResponse from(TilGithubCommit commit) {
        return new TilGithubContributionCommitResponse(
                commit.getTilGithubCommitId(),
                commit.getDailySummary().getSummaryId(),
                commit.getGithubRepositoryConnection().getGithubRepositoryId(),
                commit.getGithubRepositoryConnection().getName(),
                commit.getGithubRepositoryConnection().getFullName(),
                commit.getBranch(),
                commit.getFilePath(),
                commit.getTitle(),
                commit.getCommitSha(),
                commit.getCommitUrl(),
                commit.getPushedAt()
        );
    }
}
