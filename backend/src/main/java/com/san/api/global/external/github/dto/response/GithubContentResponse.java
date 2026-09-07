package com.san.api.global.external.github.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

/** GitHub Contents API 파일 조회 응답 DTO */
public record GithubContentResponse(
        String path,
        String sha,

        @JsonProperty("html_url")
        String htmlUrl
) {
}
