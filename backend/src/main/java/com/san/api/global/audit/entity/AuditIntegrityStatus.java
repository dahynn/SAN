package com.san.api.global.audit.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 감사 로그 무결성 검증 결과 상태입니다.
 */
@Getter
@RequiredArgsConstructor
public enum AuditIntegrityStatus {
    VALID("저장된 해시와 현재 로그 내용이 일치합니다."),
    INVALID("저장된 해시와 현재 로그 내용이 일치하지 않습니다."),
    MISSING_HASH("무결성 검증에 사용할 해시가 없습니다.");

    private final String description;
}
