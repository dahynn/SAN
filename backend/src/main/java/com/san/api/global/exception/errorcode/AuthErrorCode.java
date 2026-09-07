package com.san.api.global.exception.errorcode;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 인증/인가 도메인 에러 코드 (A 계열)
 *
 * 클라이언트에 노출되는 에러 코드는 최소한의 정보만 담습니다.
 * 구체적인 실패 원인(어떤 필드가 틀렸는지 등)은 로그에만 기록합니다.
 */
@Getter
@AllArgsConstructor
public enum AuthErrorCode implements ErrorCode {

    // Account / login
    USERNAME_ALREADY_EXISTS(HttpStatus.CONFLICT, "A001", "이미 사용 중인 아이디입니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "A002", "아이디 또는 비밀번호가 올바르지 않습니다."),
    ACCOUNT_LOCKED(HttpStatus.LOCKED, "A003", "로그인 시도 초과로 계정이 잠겼습니다. 잠시 후 다시 시도해주세요."),
    ACCOUNT_WITHDRAWN(HttpStatus.FORBIDDEN, "A004", "탈퇴한 계정입니다."),

    // Token / session
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "A101", "유효하지 않은 리프레시 토큰입니다."),
    TOKEN_BLACKLISTED(HttpStatus.UNAUTHORIZED, "A102", "이미 로그아웃된 토큰입니다."),
    INVALID_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED, "A103", "유효하지 않은 액세스 토큰입니다."),
    INVALID_CLIENT_TYPE(HttpStatus.BAD_REQUEST, "A104", "지원하지 않는 클라이언트 타입입니다."),
    INVALID_LOGIN_BRIDGE_TICKET(HttpStatus.UNAUTHORIZED, "A105", "유효하지 않은 로그인 브릿지 티켓입니다."),
    SESSION_REVOKED(HttpStatus.UNAUTHORIZED, "A106", "종료된 인증 세션입니다."),

    // GitHub / OAuth
    GITHUB_OAUTH_FAILED(HttpStatus.UNAUTHORIZED, "A201", "GitHub OAuth 인증에 실패했습니다."),
    GITHUB_ACCOUNT_NOT_LINKED(HttpStatus.BAD_REQUEST, "A202", "GitHub 계정이 연결되어 있지 않습니다."),
    GITHUB_REPOSITORY_NOT_FOUND(HttpStatus.NOT_FOUND, "A203", "GitHub 저장소를 찾을 수 없습니다."),
    GITHUB_ACCOUNT_ALREADY_LINKED(HttpStatus.CONFLICT, "A204", "이미 다른 계정에 연결된 GitHub 계정입니다."),
    GITHUB_ACCOUNT_UNLINK_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "A205", "GitHub 로그인 계정은 연동 해제할 수 없습니다."),
    GITHUB_ACCOUNT_ALREADY_LINKED_TO_CURRENT_USER(HttpStatus.CONFLICT, "A206", "현재 연동된 GitHub 계정이 존재합니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
