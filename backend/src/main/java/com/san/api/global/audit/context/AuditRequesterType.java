package com.san.api.global.audit.context;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 감사 대상 행위를 요청한 주체 유형입니다.
 */
@Getter
@RequiredArgsConstructor
public enum AuditRequesterType {
    USER("사용자"),
    ADMIN("관리자"),
    SYSTEM("시스템"),
    SCHEDULER("스케줄러");

    private final String description;
}
