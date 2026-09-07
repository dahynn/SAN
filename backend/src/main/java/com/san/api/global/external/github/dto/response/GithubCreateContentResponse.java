package com.san.api.global.external.github.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

/** GitHub Contents API 파일 생성 응답 DTO */
public record GithubCreateContentResponse(
        GithubContent content,
        GithubCommit commit
) {

    public record GithubContent(
            String path,
            String sha,

            @JsonProperty("html_url")
            String htmlUrl
    ) {
    }

    public record GithubCommit(
            String sha,

            @JsonProperty("html_url")
            String htmlUrl
    ) {
    }
}
