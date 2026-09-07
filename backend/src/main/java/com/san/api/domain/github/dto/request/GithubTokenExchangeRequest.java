package com.san.api.domain.github.dto.request;

import jakarta.validation.constraints.NotBlank;

/** GitHub OAuth 로그인 ticket 교환 요청 DTO. */
public record GithubTokenExchangeRequest(
        @NotBlank(message = "GitHub 로그인 티켓을 입력해주세요.")
        String ticket
) {
}
