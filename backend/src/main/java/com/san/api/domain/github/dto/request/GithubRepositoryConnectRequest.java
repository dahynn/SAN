package com.san.api.domain.github.dto.request;

import jakarta.validation.constraints.NotNull;

/** GitHub 레포지토리 연결 요청 DTO. */
public record GithubRepositoryConnectRequest(
        @NotNull(message = "GitHub 저장소 ID를 입력해주세요.")
        Long githubRepositoryId
) {
}
