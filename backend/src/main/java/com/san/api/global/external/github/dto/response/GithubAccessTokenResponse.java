package com.san.api.global.external.github.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

/** GitHub access token 발급 응답 DTO. */
public record GithubAccessTokenResponse(
        @JsonProperty("access_token")
        String accessToken,

        @JsonProperty("token_type")
        String tokenType,

        String scope
) {
}
