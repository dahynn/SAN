package com.san.api.global.audit.entity;

import com.san.api.global.exception.errorcode.AuthErrorCode;

import java.util.Arrays;

public enum AuditFailureReason {

    AUTH_USERNAME_ALREADY_EXISTS(AuthErrorCode.USERNAME_ALREADY_EXISTS, "AUTH.USERNAME_ALREADY_EXISTS", "이미 사용 중인 아이디입니다."),
    AUTH_INVALID_CREDENTIALS(AuthErrorCode.INVALID_CREDENTIALS, "AUTH.INVALID_CREDENTIALS", "아이디 또는 비밀번호가 올바르지 않습니다."),
    AUTH_ACCOUNT_LOCKED(AuthErrorCode.ACCOUNT_LOCKED, "AUTH.ACCOUNT_LOCKED", "계정이 잠겨 있습니다."),
    AUTH_ACCOUNT_WITHDRAWN(AuthErrorCode.ACCOUNT_WITHDRAWN, "AUTH.ACCOUNT_WITHDRAWN", "탈퇴한 계정입니다."),
    AUTH_INVALID_REFRESH_TOKEN(AuthErrorCode.INVALID_REFRESH_TOKEN, "AUTH.INVALID_REFRESH_TOKEN", "유효하지 않은 리프레시 토큰입니다."),
    AUTH_TOKEN_BLACKLISTED(AuthErrorCode.TOKEN_BLACKLISTED, "AUTH.TOKEN_BLACKLISTED", "이미 로그아웃된 토큰입니다."),
    AUTH_INVALID_ACCESS_TOKEN(AuthErrorCode.INVALID_ACCESS_TOKEN, "AUTH.INVALID_ACCESS_TOKEN", "유효하지 않은 액세스 토큰입니다."),
    AUTH_INVALID_CLIENT_TYPE(AuthErrorCode.INVALID_CLIENT_TYPE, "AUTH.INVALID_CLIENT_TYPE", "지원하지 않는 클라이언트 타입입니다."),
    AUTH_INVALID_LOGIN_BRIDGE_TICKET(AuthErrorCode.INVALID_LOGIN_BRIDGE_TICKET, "AUTH.INVALID_LOGIN_BRIDGE_TICKET", "유효하지 않은 로그인 브리지 티켓입니다."),
    AUTH_SESSION_REVOKED(AuthErrorCode.SESSION_REVOKED, "AUTH.SESSION_REVOKED", "종료된 인증 세션입니다."),
    AUTH_GITHUB_OAUTH_FAILED(AuthErrorCode.GITHUB_OAUTH_FAILED, "AUTH.GITHUB_OAUTH_FAILED", "GitHub OAuth 인증에 실패했습니다."),
    AUTH_GITHUB_ACCOUNT_NOT_LINKED(AuthErrorCode.GITHUB_ACCOUNT_NOT_LINKED, "AUTH.GITHUB_ACCOUNT_NOT_LINKED", "GitHub 계정이 연결되어 있지 않습니다."),
    AUTH_GITHUB_REPOSITORY_NOT_FOUND(AuthErrorCode.GITHUB_REPOSITORY_NOT_FOUND, "AUTH.GITHUB_REPOSITORY_NOT_FOUND", "GitHub 저장소를 찾을 수 없습니다."),
    AUTH_GITHUB_ACCOUNT_ALREADY_LINKED(AuthErrorCode.GITHUB_ACCOUNT_ALREADY_LINKED, "AUTH.GITHUB_ACCOUNT_ALREADY_LINKED", "이미 다른 계정에 연결된 GitHub 계정입니다."),
    AUTH_GITHUB_ACCOUNT_UNLINK_NOT_ALLOWED(AuthErrorCode.GITHUB_ACCOUNT_UNLINK_NOT_ALLOWED, "AUTH.GITHUB_ACCOUNT_UNLINK_NOT_ALLOWED", "GitHub 로그인 계정은 연동 해제할 수 없습니다."),
    UNKNOWN("UNKNOWN_FAILURE", "감사 로그 실패 사유를 확인할 수 없습니다.");

    private final AuthErrorCode authErrorCode;
    private final String code;
    private final String message;

    AuditFailureReason(AuthErrorCode authErrorCode, String code, String message) {
        this.authErrorCode = authErrorCode;
        this.code = code;
        this.message = message;
    }

    AuditFailureReason(String code, String message) {
        this(null, code, message);
    }

    public static AuditFailureReason from(AuthErrorCode authErrorCode) {
        if (authErrorCode == null) {
            return UNKNOWN;
        }
        return Arrays.stream(values())
                .filter(reason -> reason.authErrorCode == authErrorCode)
                .findFirst()
                .orElse(UNKNOWN);
    }

    public String code() {
        return code;
    }

    public String message() {
        return message;
    }
}
