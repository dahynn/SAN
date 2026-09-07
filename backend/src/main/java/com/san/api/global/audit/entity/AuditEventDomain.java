package com.san.api.global.audit.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 감사 로그 이벤트가 발생한 업무 도메인입니다.
 *
 * <p>도메인 단위 필터링과 통계 집계에 사용합니다.</p>
 */
@Getter
@RequiredArgsConstructor
public enum AuditEventDomain {
    AUTH("인증"),
    USER("사용자"),
    GITHUB("GitHub 연동"),
    TIL("TIL"),
    NOTIFICATION("알림"),
    OUTBOX("Outbox"),
    ASYNC_JOB("비동기 작업"),
    EXTERNAL_API("외부 API");

    private final String description;
}
