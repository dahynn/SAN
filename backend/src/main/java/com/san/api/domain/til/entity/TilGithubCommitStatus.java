package com.san.api.domain.til.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** TIL GitHub 커밋 처리 상태 */
@Getter
@RequiredArgsConstructor
public enum TilGithubCommitStatus {
    PENDING("GitHub 커밋 요청이 등록된 상태"),
    PROCESSING("GitHub 커밋 요청을 처리 중인 상태"),
    COMPLETED("GitHub 커밋이 완료된 상태"),
    FAILED("GitHub 커밋이 실패한 상태");

    private final String description;
}
