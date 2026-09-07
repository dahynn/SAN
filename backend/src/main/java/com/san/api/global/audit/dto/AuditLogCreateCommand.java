package com.san.api.global.audit.dto;

import com.san.api.global.audit.entity.AuditEventDomain;
import com.san.api.global.audit.entity.AuditEventType;
import com.san.api.global.audit.entity.AuditOutcome;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * 감사 로그 이벤트 저장 요청 값.
 * 도메인 서비스에서 발생한 사용자 행위와 외부 연동 결과를 AuditLogService로 전달할 때 사용한다.
 */
public record AuditLogCreateCommand(
        UUID actorUserId,
        String traceId,
        AuditEventDomain eventDomain,
        AuditEventType eventType,
        String targetType,
        UUID targetId,
        AuditOutcome outcome,
        String failureReasonCode,
        String failureMessage,
        String ipAddress,
        String userAgent,
        Map<String, Object> metadata
) {

    public AuditLogCreateCommand {
        Objects.requireNonNull(eventDomain, "감사 이벤트 도메인은 필수입니다.");
        Objects.requireNonNull(eventType, "감사 이벤트 유형은 필수입니다.");
        Objects.requireNonNull(outcome, "감사 처리 결과는 필수입니다.");
    }
}
