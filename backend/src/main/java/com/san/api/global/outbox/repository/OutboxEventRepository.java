package com.san.api.global.outbox.repository;

import com.san.api.global.outbox.entity.OutboxEvent;
import com.san.api.global.outbox.entity.OutboxEventStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Outbox 이벤트의 저장과 조회를 담당하는 Repository입니다. */
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID>, JpaSpecificationExecutor<OutboxEvent> {

    /**
     * 처리 가능한 대기 이벤트를 생성 순서대로 최대 100건 조회합니다.
     *
     * @param status 조회할 이벤트 상태
     * @param now    현재 시각
     * @return 처리 가능한 Outbox 이벤트 목록
     */
    List<OutboxEvent> findTop100ByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
            OutboxEventStatus status,
            LocalDateTime now
    );

    /**
     * 운영 조회를 위해 특정 상태의 이벤트를 최신순으로 조회합니다.
     *
     * @param status 조회할 이벤트 상태
     * @return 해당 상태의 Outbox 이벤트 목록
     */
    List<OutboxEvent> findByStatusOrderByCreatedAtDesc(OutboxEventStatus status);

    /**
     * 특정 시각 이전에 처리 중으로 남아 있는 이벤트를 조회합니다.
     *
     * @param status    조회할 이벤트 상태
     * @param threshold 고착 이벤트 판단 기준 시각
     * @return 오래된 처리 중 Outbox 이벤트 목록
     */
    List<OutboxEvent> findByStatusAndUpdatedAtBefore(OutboxEventStatus status, LocalDateTime threshold);

    /**
     * 특정 이벤트 ID와 상태가 모두 일치하는 이벤트를 조회합니다.
     *
     * @param outboxEventId 조회할 Outbox 이벤트 ID
     * @param status        조회할 이벤트 상태
     * @return 상태가 일치하는 Outbox 이벤트
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<OutboxEvent> findByOutboxEventIdAndStatus(UUID outboxEventId, OutboxEventStatus status);
}
