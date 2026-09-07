package com.san.api.global.audit.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 감사 로그 대상 리소스 유형입니다.
 */
@Getter
@RequiredArgsConstructor
public enum AuditTargetType {
    USER("사용자"),
    GITHUB_ACCOUNT("GitHub 계정"),
    GITHUB_REPOSITORY("GitHub 저장소"),
    DAILY_SUMMARY("TIL 요약"),
    TIL_GITHUB_COMMIT("TIL GitHub 커밋 요청"),
    OUTBOX_EVENT("Outbox 이벤트"),
    ASYNC_JOB("비동기 작업");

    private final String description;

    public String code() {
        return name();
    }
}
