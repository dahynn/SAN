package com.san.api.global.external.github.dto.request;

/** GitHub Contents API 파일 생성 요청 DTO */
public record GithubCreateContentRequest(
        String message,
        String content,
        String branch
) {
}
