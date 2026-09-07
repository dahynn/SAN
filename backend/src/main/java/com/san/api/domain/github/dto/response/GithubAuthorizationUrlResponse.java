package com.san.api.domain.github.dto.response;

/** GitHub OAuth authorize URL 응답 DTO */
public record GithubAuthorizationUrlResponse(
        String redirectUrl
) {
}
