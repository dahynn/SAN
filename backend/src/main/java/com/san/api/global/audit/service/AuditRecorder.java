package com.san.api.global.audit.service;

import com.san.api.global.audit.context.AuditRequestContext;
import com.san.api.global.audit.context.AuditRequestContextHolder;
import com.san.api.global.audit.dto.AuditLogCreateCommand;
import com.san.api.global.audit.dto.AuditRecordCommand;
import com.san.api.global.audit.entity.AuditOutcome;
import com.san.api.global.exception.errorcode.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditRecorder {

    private final AuditLogService auditLogService;

    /**
     * 성공한 업무 행위를 감사 로그로 저장합니다.
     *
     * @param command 감사 이벤트 도메인, 유형, 대상, 요청 컨텍스트를 담은 명령
     * @return 저장된 감사 로그 이벤트 ID
     */
    public UUID recordSuccess(AuditRecordCommand command) {
        return auditLogService.save(toCreateCommand(command, AuditOutcome.SUCCESS, null, null, command.metadata()));
    }

    /**
     * 실패한 업무 행위를 실패 사유 코드와 메시지로 감사 로그에 저장합니다.
     *
     * @param command 감사 이벤트 도메인, 유형, 대상, 요청 컨텍스트를 담은 명령
     * @param failureReasonCode 감사 로그에 남길 실패 사유 코드
     * @param failureMessage 감사 로그에 남길 실패 메시지
     * @return 저장된 감사 로그 이벤트 ID
     */
    public UUID recordFailure(AuditRecordCommand command, String failureReasonCode, String failureMessage) {
        return auditLogService.save(toCreateCommand(
                command,
                AuditOutcome.FAILURE,
                failureReasonCode,
                failureMessage,
                command.metadata()
        ));
    }

    /**
     * 실패한 업무 행위를 서비스 표준 에러 코드 기반으로 감사 로그에 저장합니다.
     *
     * @param command 감사 이벤트 도메인, 유형, 대상, 요청 컨텍스트를 담은 명령
     * @param errorCode 실패 사유로 사용할 서비스 표준 에러 코드
     * @return 저장된 감사 로그 이벤트 ID
     */
    public UUID recordFailure(AuditRecordCommand command, ErrorCode errorCode) {
        return auditLogService.save(toCreateCommand(
                command,
                AuditOutcome.FAILURE,
                failureReasonCode(errorCode),
                errorCode == null ? null : errorCode.getMessage(),
                failureMetadata(command.metadata(), errorCode)
        ));
    }

    /**
     * 성공 감사 로그를 저장하되, 저장 실패가 원래 업무 흐름으로 전파되지 않도록 방어합니다.
     *
     * @param command 감사 이벤트 도메인, 유형, 대상, 요청 컨텍스트를 담은 명령
     * @return 저장에 성공하면 감사 로그 이벤트 ID, 실패하면 빈 Optional
     */
    public Optional<UUID> recordSuccessSafely(AuditRecordCommand command) {
        return saveSafely(command, AuditOutcome.SUCCESS, null, null, command.metadata());
    }

    /**
     * 실패 감사 로그를 저장하되, 저장 실패가 원래 업무 흐름으로 전파되지 않도록 방어합니다.
     *
     * @param command 감사 이벤트 도메인, 유형, 대상, 요청 컨텍스트를 담은 명령
     * @param failureReasonCode 감사 로그에 남길 실패 사유 코드
     * @param failureMessage 감사 로그에 남길 실패 메시지
     * @return 저장에 성공하면 감사 로그 이벤트 ID, 실패하면 빈 Optional
     */
    public Optional<UUID> recordFailureSafely(
            AuditRecordCommand command,
            String failureReasonCode,
            String failureMessage
    ) {
        return saveSafely(command, AuditOutcome.FAILURE, failureReasonCode, failureMessage, command.metadata());
    }

    /**
     * 실패 감사 로그를 서비스 표준 에러 코드 기반으로 저장하되, 저장 실패를 원래 업무 흐름과 분리합니다.
     *
     * @param command 감사 이벤트 도메인, 유형, 대상, 요청 컨텍스트를 담은 명령
     * @param errorCode 실패 사유로 사용할 서비스 표준 에러 코드
     * @return 저장에 성공하면 감사 로그 이벤트 ID, 실패하면 빈 Optional
     */
    public Optional<UUID> recordFailureSafely(AuditRecordCommand command, ErrorCode errorCode) {
        return saveSafely(
                command,
                AuditOutcome.FAILURE,
                failureReasonCode(errorCode),
                errorCode == null ? null : errorCode.getMessage(),
                failureMetadata(command.metadata(), errorCode)
        );
    }

    private Optional<UUID> saveSafely(
            AuditRecordCommand command,
            AuditOutcome outcome,
            String failureReasonCode,
            String failureMessage,
            Map<String, Object> metadata
    ) {
        try {
            return Optional.of(auditLogService.save(toCreateCommand(
                    command,
                    outcome,
                    failureReasonCode,
                    failureMessage,
                    metadata
            )));
        } catch (RuntimeException e) {
            log.warn(
                    "감사 로그 저장 실패 - eventDomain={}, eventType={}, outcome={}, actorUserId={}",
                    command.eventDomain(),
                    command.eventType(),
                    outcome,
                    command.actorUserId(),
                    e
            );
            return Optional.empty();
        }
    }

    private AuditLogCreateCommand toCreateCommand(
            AuditRecordCommand command,
            AuditOutcome outcome,
            String failureReasonCode,
            String failureMessage,
            Map<String, Object> metadata
    ) {
        AuditRequestContext context = AuditRequestContextHolder.get().orElse(null);
        return new AuditLogCreateCommand(
                command.actorUserId(),
                firstNonBlank(command.traceId(), context == null ? null : context.traceId()),
                command.eventDomain(),
                command.eventType(),
                command.targetType(),
                command.targetId(),
                outcome,
                failureReasonCode,
                failureMessage,
                firstNonBlank(command.ipAddress(), context == null ? null : context.ipAddress()),
                firstNonBlank(command.userAgent(), context == null ? null : context.userAgent()),
                metadata
        );
    }

    private String firstNonBlank(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary;
        }
        return fallback;
    }

    private String failureReasonCode(ErrorCode errorCode) {
        if (errorCode == null) {
            return "UNKNOWN_FAILURE";
        }
        return errorCode.getCode();
    }

    private Map<String, Object> failureMetadata(Map<String, Object> metadata, ErrorCode errorCode) {
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
