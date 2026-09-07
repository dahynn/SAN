package com.san.api.global.audit.repository;

import com.san.api.global.audit.entity.AuditEventDomain;
import com.san.api.global.audit.entity.AuditEventType;
import com.san.api.global.audit.entity.AuditLogEvent;
import com.san.api.global.audit.entity.AuditOutcome;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 감사 로그 이벤트 저장소.
 * 사용자 행위, 외부 연동 성공/실패, 비동기 처리 흐름에서 발생한 감사 이벤트를 저장하고 조회
 */
public interface AuditLogEventRepository extends JpaRepository<AuditLogEvent, UUID>,
        JpaSpecificationExecutor<AuditLogEvent> {

    long countByOccurredAtBetween(LocalDateTime from, LocalDateTime to);

    @Query("""
            SELECT e.outcome AS outcome, COUNT(e) AS count
            FROM AuditLogEvent e
            WHERE e.occurredAt BETWEEN :from AND :to
            GROUP BY e.outcome
            """)
    List<OutcomeCountProjection> countByOutcomeBetween(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    @Query("""
            SELECT e.eventDomain AS eventDomain, COUNT(e) AS count
            FROM AuditLogEvent e
            WHERE e.occurredAt BETWEEN :from AND :to
            GROUP BY e.eventDomain
            ORDER BY COUNT(e) DESC
            """)
    List<DomainCountProjection> countByEventDomainBetween(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    @Query("""
            SELECT e.eventType AS eventType, COUNT(e) AS count
            FROM AuditLogEvent e
            WHERE e.eventDomain = :eventDomain
              AND e.occurredAt BETWEEN :from AND :to
            GROUP BY e.eventType
            """)
    List<EventTypeCountProjection> countByEventTypeInDomainBetween(
            @Param("eventDomain") AuditEventDomain eventDomain,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    @Query("""
            SELECT e.failureReasonCode AS failureReasonCode, COUNT(e) AS count
            FROM AuditLogEvent e
            WHERE e.outcome = :outcome
              AND e.failureReasonCode IS NOT NULL
              AND e.failureReasonCode <> ''
              AND e.occurredAt BETWEEN :from AND :to
            GROUP BY e.failureReasonCode
            ORDER BY COUNT(e) DESC
            """)
    List<FailureReasonCountProjection> findTopFailureReasonsBetween(
            @Param("outcome") AuditOutcome outcome,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable
    );

    List<AuditLogEvent> findByOutcomeAndOccurredAtBetweenOrderByOccurredAtDesc(
            AuditOutcome outcome,
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable
    );

    interface OutcomeCountProjection {
        AuditOutcome getOutcome();

        long getCount();
    }

    interface DomainCountProjection {
        AuditEventDomain getEventDomain();

        long getCount();
    }

    interface EventTypeCountProjection {
        AuditEventType getEventType();

        long getCount();
    }

    interface FailureReasonCountProjection {
        String getFailureReasonCode();

        long getCount();
    }
}
