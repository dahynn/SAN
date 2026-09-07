package com.san.api.domain.auth.service;

import com.san.api.global.audit.context.AuditRequestContext;
import com.san.api.global.audit.context.AuditRequestContextHolder;
import com.san.api.global.audit.dto.AuditLogCreateCommand;
import com.san.api.global.audit.entity.AuditEventDomain;
import com.san.api.global.audit.entity.AuditEventType;
import com.san.api.global.audit.entity.AuditFailureReason;
import com.san.api.global.audit.entity.AuditOutcome;
import com.san.api.global.audit.entity.AuditTargetType;
import com.san.api.global.audit.service.AuditLogService;
import com.san.api.global.exception.errorcode.AuthErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Auth 도메인 감사 이벤트를 구조화해 저장하는 전용 서비스.
 *
 * AuthService가 인증 흐름에만 집중할 수 있도록 AuditLogCreateCommand 조립,
 * 요청 컨텍스트(traceId, IP, User-Agent) 주입, 저장 실패 방어를 담당한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthAuditService {

    private final AuditLogService auditLogService;

    public void recordSuccess(
            UUID actorUserId,
            AuditEventType eventType,
            UUID targetId,
            Map<String, Object> metadata
    ) {
        saveSafely(newCommand(
                actorUserId,
                eventType,
                targetId,
                AuditOutcome.SUCCESS,
                null,
                null,
                metadata
        ));
    }

    public void recordFailure(
            UUID actorUserId,
            AuditEventType eventType,
            AuthErrorCode errorCode,
            Map<String, Object> metadata
    ) {
        recordFailure(actorUserId, eventType, actorUserId, errorCode, metadata);
    }

    public void recordFailure(
            UUID actorUserId,
            AuditEventType eventType,
            UUID targetId,
            AuthErrorCode errorCode,
            Map<String, Object> metadata
    ) {
        AuditFailureReason failureReason = AuditFailureReason.from(errorCode);
        saveSafely(newCommand(
                actorUserId,
                eventType,
                targetId,
                AuditOutcome.FAILURE,
                failureReason.code(),
                failureReason.message(),
                failureMetadata(errorCode, metadata)
        ));
    }

    private AuditLogCreateCommand newCommand(
            UUID actorUserId,
            AuditEventType eventType,
            UUID targetId,
            AuditOutcome outcome,
            String failureReasonCode,
            String failureMessage,
            Map<String, Object> metadata
    ) {
        AuditRequestContext context = AuditRequestContextHolder.get().orElse(null);
        return new AuditLogCreateCommand(
                actorUserId,
                context == null ? null : context.traceId(),
                AuditEventDomain.AUTH,
                eventType,
                AuditTargetType.USER.code(),
                targetId,
                outcome,
                failureReasonCode,
                failureMessage,
                context == null ? null : context.ipAddress(),
                context == null ? null : context.userAgent(),
                metadata
        );
    }

    private void saveSafely(AuditLogCreateCommand command) {
        try {
            auditLogService.save(command);
        } catch (RuntimeException e) {
            log.warn(
                    "[Auth] 감사 이벤트 저장 실패 - eventType={}, outcome={}, actorUserId={}",
                    command.eventType(),
                    command.outcome(),
                    command.actorUserId(),
                    e
            );
        }
    }

    private Map<String, Object> failureMetadata(AuthErrorCode errorCode, Map<String, Object> metadata) {
        Map<String, Object> merged = new LinkedHashMap<>();
        if (metadata != null) {
            merged.putAll(metadata);
        }
        if (errorCode != null) {
            merged.put("clientErrorCode", errorCode.getCode());
            merged.put("httpStatus", errorCode.getStatus().value());
        }
        return merged;
    }
}
