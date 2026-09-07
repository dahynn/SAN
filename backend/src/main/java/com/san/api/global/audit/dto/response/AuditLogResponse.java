package com.san.api.global.audit.dto.response;

import com.san.api.global.audit.entity.AuditEventDomain;
import com.san.api.global.audit.entity.AuditEventType;
import com.san.api.global.audit.entity.AuditLogEvent;
import com.san.api.global.audit.entity.AuditOutcome;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public record AuditLogResponse(
        UUID auditLogEventId,
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
        Map<String, Object> metadata,
        LocalDateTime occurredAt
) {

    public static AuditLogResponse from(AuditLogEvent event) {
        return new AuditLogResponse(
                event.getAuditLogEventId(),
                event.getActorUserId(),
                event.getTraceId(),
                event.getEventDomain(),
                event.getEventType(),
                event.getTargetType(),
                event.getTargetId(),
                event.getOutcome(),
                event.getFailureReasonCode(),
                event.getFailureMessage(),
                event.getIpAddress(),
                event.getUserAgent(),
                event.getMetadata(),
                event.getOccurredAt()
        );
    }
}
