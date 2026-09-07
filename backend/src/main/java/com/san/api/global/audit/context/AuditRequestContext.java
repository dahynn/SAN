package com.san.api.global.audit.context;

/**
 * 감사 로그에 함께 남길 HTTP 요청 단위 컨텍스트.
 *
 * 요청 필터에서 traceId, IP, User-Agent를 추출해 담고
 * 도메인 서비스의 감사 이벤트 저장 시 함께 사용한다.
 */
public record AuditRequestContext(
        String traceId,
        String ipAddress,
        String userAgent
) {
}
