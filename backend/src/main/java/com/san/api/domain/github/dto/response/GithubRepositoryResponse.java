package com.san.api.domain.github.dto.response;

import com.san.api.domain.github.entity.GithubRepositoryConnection;
import com.san.api.global.external.github.dto.response.ExternalGithubRepositoryResponse;

/** GitHub 레포지토리 조회 및 연결 결과 응답 DTO. */
public record GithubRepositoryResponse(
        Long githubRepositoryId,
        String name,
        String fullName,
        boolean privateRepository,
        String defaultBranch,
        String htmlUrl
) {
    public static GithubRepositoryResponse from(ExternalGithubRepositoryResponse repository) {
        return new GithubRepositoryResponse(
                repository.id(),
                repository.name(),
                repository.fullName(),
                repository.privateRepository(),
                repository.defaultBranch(),
                repository.htmlUrl()
        );
    }

    public static GithubRepositoryResponse from(GithubRepositoryConnection connection) {
        return new GithubRepositoryResponse(
                connection.getGithubRepositoryId(),
                connection.getName(),
                connection.getFullName(),
                connection.isPrivateRepository(),
                connection.getDefaultBranch(),
                connection.getHtmlUrl()
        );
    }
}
