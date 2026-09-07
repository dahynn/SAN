package com.san.api.domain.til.service;

import com.san.api.global.audit.context.AuditRequestContext;
import com.san.api.global.audit.context.AuditRequestContextHolder;
import com.san.api.global.audit.dto.AuditLogCreateCommand;
import com.san.api.global.audit.entity.AuditEventDomain;
import com.san.api.global.audit.entity.AuditEventType;
import com.san.api.global.audit.entity.AuditOutcome;
import com.san.api.global.audit.entity.AuditTargetType;
import com.san.api.global.audit.service.AuditLogService;
import com.san.api.global.exception.errorcode.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * TIL 도메인의 감사 이벤트를 생성해 저장하는 서비스입니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TilAuditService {

    private static final String FAILURE_REASON_PREFIX = "TIL.";

    private final AuditLogService auditLogService;

    /**
     * TIL 도메인 성공 이벤트를 감사 로그로 저장합니다.
     *
     * @param actorUserId 행위를 수행한 사용자 ID
     * @param eventType 기록할 감사 이벤트 유형
     * @param targetType 감사 대상 리소스 유형
     * @param targetId 감사 대상 리소스 ID
     * @param metadata 추가 분석용 메타데이터
     */
    public void recordSuccess(
            UUID actorUserId,
            AuditEventType eventType,
            AuditTargetType targetType,
            UUID targetId,
            Map<String, Object> metadata
    ) {
        saveSafely(newCommand(
                actorUserId,
                eventType,
                targetType,
                targetId,
                AuditOutcome.SUCCESS,
                null,
                null,
                metadata
        ));
    }

    /**
     * TIL 도메인 실패 이벤트를 도메인 에러 코드 기반 실패 사유와 함께 감사 로그로 저장합니다.
     *
     * @param actorUserId 행위를 수행한 사용자 ID
     * @param eventType 기록할 감사 이벤트 유형
     * @param targetType 감사 대상 리소스 유형
     * @param targetId 감사 대상 리소스 ID
     * @param errorCode 실패 원인이 된 도메인 에러 코드
     * @param metadata 추가 분석용 메타데이터
     */
    public void recordFailure(
            UUID actorUserId,
            AuditEventType eventType,
            AuditTargetType targetType,
            UUID targetId,
            ErrorCode errorCode,
            Map<String, Object> metadata
    ) {
        saveSafely(newCommand(
                actorUserId,
                eventType,
                targetType,
                targetId,
                AuditOutcome.FAILURE,
                failureReasonCode(errorCode),
                errorCode == null ? null : errorCode.getMessage(),
                failureMetadata(errorCode, metadata)
        ));
    }

    /**
     * 비동기 처리 중 발생한 예외처럼 별도 ErrorCode가 없는 실패 이벤트를 감사 로그로 저장합니다.
     *
     * @param actorUserId 행위를 수행한 사용자 ID
     * @param eventType 기록할 감사 이벤트 유형
     * @param targetType 감사 대상 리소스 유형
     * @param targetId 감사 대상 리소스 ID
     * @param failureReasonCode 감사 로그에 저장할 실패 사유 코드
     * @param failureMessage 감사 로그에 저장할 실패 메시지
     * @param metadata 추가 분석용 메타데이터
     */
    public void recordFailure(
            UUID actorUserId,
            AuditEventType eventType,
            AuditTargetType targetType,
            UUID targetId,
            String failureReasonCode,
            String failureMessage,
            Map<String, Object> metadata
    ) {
        saveSafely(newCommand(
                actorUserId,
                eventType,
                targetType,
                targetId,
                AuditOutcome.FAILURE,
                failureReasonCode,
                failureMessage,
                metadata
        ));
    }

    private AuditLogCreateCommand newCommand(
            UUID actorUserId,
            AuditEventType eventType,
            AuditTargetType targetType,
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
                AuditEventDomain.TIL,
                eventType,
                targetType.code(),
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
                    "[TIL] 감사 이벤트 저장 실패 - eventType={}, outcome={}, actorUserId={}",
                    command.eventType(),
                    command.outcome(),
                    command.actorUserId(),
                    e
            );
        }
    }

    private String failureReasonCode(ErrorCode errorCode) {
        if (errorCode == null) {
            return FAILURE_REASON_PREFIX + "UNKNOWN_FAILURE";
        }
        return FAILURE_REASON_PREFIX + errorCode.getCode();
    }

    private Map<String, Object> failureMetadata(ErrorCode errorCode, Map<String, Object> metadata) {
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
