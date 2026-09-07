package com.san.api.domain.user.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserStatus {
    ACTIVE("정상 상태"),
    LOCKED("일시적 잠금(lockedUntil까지)"),
    WITHDRAWN("탈퇴(논리 삭제)");

    private final String description;
}
