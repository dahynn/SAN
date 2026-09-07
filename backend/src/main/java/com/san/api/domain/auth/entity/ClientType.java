package com.san.api.domain.auth.entity;

import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.AuthErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Locale;

/** 인증 세션을 사용하는 클라이언트 종류를 구분합니다. */
@Getter
@RequiredArgsConstructor
public enum ClientType {
    DASHBOARD("웹 대시보드"),
    EXTENSION("브라우저 확장 프로그램");

    private final String description;

    /** 요청으로 전달된 clientType 값을 enum으로 변환합니다. */
    public static ClientType from(String value) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(AuthErrorCode.INVALID_CLIENT_TYPE);
        }

        try {
            return ClientType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new BusinessException(AuthErrorCode.INVALID_CLIENT_TYPE);
        }
    }
}
