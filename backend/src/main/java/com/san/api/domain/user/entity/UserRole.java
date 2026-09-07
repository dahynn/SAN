package com.san.api.domain.user.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 사용자에게 부여되는 시스템 접근 권한입니다.
 */
@Getter
@RequiredArgsConstructor
public enum UserRole {
    USER("일반 사용자"),
    ADMIN("관리자");

    private final String description;
}
