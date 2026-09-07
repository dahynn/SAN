package com.san.api.global.audit.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 감사 로그에 기록할 업무 이벤트 유형입니다.
 *
 * <p>사용자 주요 행위와 외부 연동 처리 결과를 코드화해 저장합니다.
 * 실패 이벤트는 failureReasonCode, metadata, traceId와 함께 원인 분석에 활용합니다.</p>
 */
@Getter
@RequiredArgsConstructor
public enum AuditEventType {
    LOGIN_SUCCESS("로그인 성공"),
    LOGIN_FAILURE("로그인 실패"),
    LOGIN_LOCK_TRIGGERED("로그인 실패 누적으로 계정 잠금"),

    LOGOUT_SUCCESS("로그아웃 성공"),
    LOGOUT_FAILURE("로그아웃 실패"),

    LOGIN_BRIDGE_TICKET_ISSUED("로그인 브릿지 티켓 발급"),
    LOGIN_BRIDGE_TICKET_ISSUE_FAILED("로그인 브릿지 티켓 발급 실패"),

    LOGIN_BRIDGE_TOKEN_EXCHANGED("로그인 브릿지 토큰 교환"),
    LOGIN_BRIDGE_TOKEN_EXCHANGE_FAILED("로그인 브릿지 토큰 교환 실패"),

    TOKEN_REISSUE_SUCCESS("토큰 재발급 성공"),
    TOKEN_REISSUE_FAILURE("토큰 재발급 실패"),
    TOKEN_REUSE_DETECTED("리프레시 토큰 재사용 의심"),

    SESSION_REVOKED("인증 세션 수동 폐기"),

    SIGNUP("회원가입"),
    WITHDRAW("회원탈퇴"),

    GITHUB_TOKEN_LINKED("GitHub 토큰 연동"),
    GITHUB_TOKEN_REFRESH_FAILED("GitHub 토큰 갱신 실패"),

    GITHUB_API_REQUESTED("GitHub API 요청"),
    GITHUB_API_SUCCEEDED("GitHub API 요청 성공"),
    GITHUB_API_FAILED("GitHub API 요청 실패"),

    TIL_COMMIT_REQUESTED("TIL 커밋 요청"),
    TIL_COMMIT_PROCESSING("TIL 커밋 처리 중"),
    TIL_COMMIT_SUCCEEDED("TIL 커밋 성공"),
    TIL_COMMIT_FAILED("TIL 커밋 실패"),
    TIL_COMMIT_RETRY_REQUESTED("TIL 커밋 재시도 요청"),
    TIL_COMMIT_DUPLICATE_BLOCKED("TIL 커밋 중복 차단"),

    NOTIFICATION_SENT("알림 발송"),
    NOTIFICATION_READ("알림 읽음"),
    NOTIFICATION_STATUS_CHANGED("알림 상태 변경"),

    OUTBOX_EVENT_RETRY_REQUESTED("Outbox 이벤트 수동 재처리 요청"),

    ASYNC_JOB_PROCESSING("비동기 작업 처리 중"),
    ASYNC_JOB_SUCCEEDED("비동기 작업 성공"),
    ASYNC_JOB_FAILED("비동기 작업 실패"),
    ASYNC_JOB_RETRY_REQUESTED("비동기 작업 재시도 요청"),
    ASYNC_JOB_DUPLICATE_BLOCKED("비동기 작업 중복 차단"),

    EXTERNAL_API_REQUESTED("외부 API 요청"),
    EXTERNAL_API_SUCCEEDED("외부 API 요청 성공"),
    EXTERNAL_API_FAILED("외부 API 요청 실패");

    private final String description;
}
