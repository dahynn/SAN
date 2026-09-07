package com.san.api.global.outbox.service;

import com.san.api.global.audit.context.AuditRequestContext;
import com.san.api.global.audit.context.AuditRequestContextHolder;
import com.san.api.global.audit.dto.AuditLogCreateCommand;
import com.san.api.global.audit.entity.AuditEventDomain;
import com.san.api.global.audit.entity.AuditEventType;
import com.san.api.global.audit.entity.AuditOutcome;
import com.san.api.global.audit.entity.AuditTargetType;
import com.san.api.global.audit.service.AuditLogService;
import com.san.api.global.outbox.entity.OutboxEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Outbox 운영 행위에 대한 감사 로그를 저장합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxEventAuditService {

    private final AuditLogService auditLogService;

    /**
     * 운영자가 실패한 Outbox 이벤트의 수동 재처리를 요청한 사실을 기록합니다.
     *
     * @param actorUserId 재처리를 요청한 관리자 사용자 ID
     * @param outboxEvent 재처리 대상 Outbox 이벤트
     */
    public void recordRetryRequested(UUID actorUserId, OutboxEvent outboxEvent) {
        AuditRequestContext context = AuditRequestContextHolder.get().orElse(null);
        saveSafely(new AuditLogCreateCommand(
                actorUserId,
                context == null ? null : context.traceId(),
                AuditEventDomain.OUTBOX,
                AuditEventType.OUTBOX_EVENT_RETRY_REQUESTED,
                AuditTargetType.OUTBOX_EVENT.code(),
                outboxEvent.getOutboxEventId(),
                AuditOutcome.SUCCESS,
                null,
                null,
                context == null ? null : context.ipAddress(),
                context == null ? null : context.userAgent(),
                retryMetadata(outboxEvent)
        ));
    }

    private Map<String, Object> retryMetadata(OutboxEvent outboxEvent) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("eventType", outboxEvent.getEventType().name());
        metadata.put("aggregateType", outboxEvent.getAggregateType());
        metadata.put("aggregateId", outboxEvent.getAggregateId());
        metadata.put("status", outboxEvent.getStatus().name());
        metadata.put("retryCount", outboxEvent.getRetryCount());
        metadata.put("maxRetryCount", outboxEvent.getMaxRetryCount());
        metadata.put("nextAttemptAt", outboxEvent.getNextAttemptAt().toString());
        return metadata;
    }

    private void saveSafely(AuditLogCreateCommand command) {
        try {
            auditLogService.save(command);
        } catch (RuntimeException e) {
            log.warn(
                    "[Outbox] 감사 이벤트 저장 실패 - eventType={}, actorUserId={}, targetId={}",
                    command.eventType(),
                    command.actorUserId(),
                    command.targetId(),
                    e
            );
        }
    }
}
