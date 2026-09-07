package com.san.api.global.external.github.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

/** 외부 GitHub API 레포지토리 응답 DTO. */
public record ExternalGithubRepositoryResponse(
        Long id,
        String name,

        @JsonProperty("full_name")
        String fullName,

        @JsonProperty("private")
        boolean privateRepository,

        @JsonProperty("default_branch")
        String defaultBranch,

        @JsonProperty("html_url")
        String htmlUrl
) {
}
