package com.san.api.global.audit.entity;

import com.san.api.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

/**
 * 사용자 주요 행위와 외부 연동 처리 결과를 저장하는 감사 로그 이벤트 엔티티.
 * 감사 로그는 기록 자체가 이력이므로 수정/삭제 흐름을 제공하는 {@code BaseEntity}를 상속하지 않는다.
 */
@Getter
@Entity
@Table(
        name = "audit_log_events",
        indexes = {
                @Index(name = "idx_audit_log_events_actor_time", columnList = "actor_user_id, occurred_at"),
                @Index(name = "idx_audit_log_events_trace_id", columnList = "trace_id"),
                @Index(name = "idx_audit_log_events_event_type_time", columnList = "event_type, occurred_at"),
                @Index(name = "idx_audit_log_events_event_domain_time", columnList = "event_domain, occurred_at"),
                @Index(name = "idx_audit_log_events_outcome_time", columnList = "outcome, occurred_at"),
                @Index(name = "idx_audit_log_events_failure_reason_time", columnList = "failure_reason_code, occurred_at"),
                @Index(name = "idx_audit_log_events_target_time", columnList = "target_type, target_id, occurred_at"),
                @Index(name = "idx_audit_log_events_occurred_at", columnList = "occurred_at")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuditLogEvent {

    private static final ChronoUnit DATABASE_TIMESTAMP_PRECISION = ChronoUnit.MICROS;

    @Id
    @Column(name = "audit_log_event_id", columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID auditLogEventId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_user_id")
    private User actorUser;

    @Column(name = "actor_user_id", columnDefinition = "uuid", insertable = false, updatable = false)
    private UUID actorUserId;

    @Column(name = "trace_id", length = 100)
    private String traceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_domain", nullable = false, length = 50)
    private AuditEventDomain eventDomain;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 100)
    private AuditEventType eventType;

    @Column(name = "target_type", length = 50)
    private String targetType;

    @Column(name = "target_id", columnDefinition = "uuid")
    private UUID targetId;

    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", nullable = false, length = 20)
    private AuditOutcome outcome;

    @Column(name = "failure_reason_code", length = 100)
    private String failureReasonCode;

    @Column(name = "failure_message", columnDefinition = "text")
    private String failureMessage;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "text")
    private String userAgent;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @Column(name = "integrity_hash", length = 64)
    private String integrityHash;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private LocalDateTime occurredAt;

    @Builder
    private AuditLogEvent(
            User actorUser,
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
        this.auditLogEventId = UUID.randomUUID();
        this.actorUser = actorUser;
        this.actorUserId = actorUser == null ? null : actorUser.getUserId();
        this.traceId = traceId;
        this.eventDomain = eventDomain;
        this.eventType = eventType;
        this.targetType = targetType;
        this.targetId = targetId;
        this.outcome = outcome;
        this.failureReasonCode = failureReasonCode;
        this.failureMessage = failureMessage;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.metadata = metadata;
        this.occurredAt = LocalDateTime.now().truncatedTo(DATABASE_TIMESTAMP_PRECISION);
    }

    public void updateIntegrityHash(String integrityHash) {
        this.integrityHash = integrityHash;
    }
}
