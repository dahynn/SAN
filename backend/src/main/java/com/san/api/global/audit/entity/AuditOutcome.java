package com.san.api.global.audit.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 감사 이벤트 처리 결과 */
@Getter
@RequiredArgsConstructor
public enum AuditOutcome {
    SUCCESS("성공"),
    FAILURE("실패");

    private final String description;
}
