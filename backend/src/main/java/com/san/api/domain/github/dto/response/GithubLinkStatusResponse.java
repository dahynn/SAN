package com.san.api.domain.github.dto.response;

import com.san.api.domain.github.entity.GithubAccount;
import com.san.api.domain.github.entity.GithubRepositoryConnection;

/** GitHub 계정과 레포연결 상태 response DTO. */
public record GithubLinkStatusResponse(
        boolean linked,
        String githubUsername,
        boolean repositoryConnected,
        GithubRepositoryResponse connectedRepository
) {
    public static GithubLinkStatusResponse notLinked() {
        return new GithubLinkStatusResponse(false, null, false, null);
    }

    public static GithubLinkStatusResponse from(
            GithubAccount githubAccount,
            GithubRepositoryConnection repositoryConnection
    ) {
        return new GithubLinkStatusResponse(
                true,
                githubAccount.getGithubUsername(),
                repositoryConnection != null,
                repositoryConnection == null ? null : GithubRepositoryResponse.from(repositoryConnection)
        );
    }
}
