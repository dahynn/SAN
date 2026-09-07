package com.san.api.global.audit.dto;

import com.san.api.global.audit.entity.AuditEventDomain;
import com.san.api.global.audit.entity.AuditEventType;
import lombok.Builder;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Builder
public record AuditRecordCommand(
        UUID actorUserId,
        String traceId,
        AuditEventDomain eventDomain,
        AuditEventType eventType,
        String targetType,
        UUID targetId,
        String ipAddress,
        String userAgent,
        Map<String, Object> metadata
) {

    public AuditRecordCommand {
        Objects.requireNonNull(eventDomain, "감사 이벤트 도메인은 필수입니다.");
        Objects.requireNonNull(eventType, "감사 이벤트 유형은 필수입니다.");
    }
}
