package com.san.api.global.external.github.dto.response;

/** 외부 GitHub API 사용자 프로필 응답 DTO. */
public record GithubUserProfileResponse(
        Long id,
        String login
) {
}
