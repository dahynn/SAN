package com.san.api.global.audit.service;

import com.san.api.domain.user.entity.User;
import com.san.api.global.audit.dto.AuditLogCreateCommand;
import com.san.api.global.audit.entity.AuditLogEvent;
import com.san.api.global.audit.repository.AuditLogEventRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 감사 로그 이벤트 저장 서비스.
 * 각 도메인의 사용자 주요 행위와 외부 연동 처리 결과를 공통 방식으로 audit_log_events 테이블에 저장한다.
 */
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final EntityManager entityManager;
    private final AuditLogEventRepository auditLogEventRepository;
    private final AuditLogIntegrityHasher auditLogIntegrityHasher;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public UUID save(AuditLogCreateCommand command) {
        AuditLogCreateCommand sanitizedCommand = AuditLogSanitizer.sanitize(command);
        AuditLogEvent event = AuditLogEvent.builder()
                .actorUser(getActorUserReference(sanitizedCommand.actorUserId()))
                .traceId(sanitizedCommand.traceId())
                .eventDomain(sanitizedCommand.eventDomain())
                .eventType(sanitizedCommand.eventType())
                .targetType(sanitizedCommand.targetType())
                .targetId(sanitizedCommand.targetId())
                .outcome(sanitizedCommand.outcome())
                .failureReasonCode(sanitizedCommand.failureReasonCode())
                .failureMessage(sanitizedCommand.failureMessage())
                .ipAddress(sanitizedCommand.ipAddress())
                .userAgent(sanitizedCommand.userAgent())
                .metadata(sanitizedCommand.metadata())
                .build();
        event.updateIntegrityHash(auditLogIntegrityHasher.hash(event));

        return auditLogEventRepository.save(event).getAuditLogEventId();
    }

    private User getActorUserReference(UUID actorUserId) {
        if (actorUserId == null) {
            return null;
        }
        return entityManager.getReference(User.class, actorUserId);
    }
}
