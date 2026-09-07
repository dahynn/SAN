package com.san.api.global.security.handler;

/**
 * Security 필터와 Security Handler 사이에서 인증 실패 사유를 전달할 때 사용하는
 * request attribute 이름을 관리합니다.
 */
public final class SecurityErrorAttribute {

    /** request에 저장되는 {@code ErrorCode} attribute 이름. */
    public static final String ERROR_CODE = "security.errorCode";

    private SecurityErrorAttribute() {
    }
}
