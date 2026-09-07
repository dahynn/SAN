package com.san.api.global.outbox.service;

import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.CommonErrorCode;
import com.san.api.global.outbox.dto.response.OutboxEventResponse;
import com.san.api.global.outbox.entity.OutboxEvent;
import com.san.api.global.outbox.entity.OutboxEventStatus;
import com.san.api.global.outbox.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 운영자가 실패한 Outbox 이벤트를 다시 처리 대기 상태로 되돌립니다.
 */
@Service
@RequiredArgsConstructor
public class OutboxEventRetryService {

    private final OutboxEventRepository outboxEventRepository;
    private final OutboxEventAuditService outboxEventAuditService;

    /**
     * FAILED 상태의 Outbox 이벤트를 즉시 재처리 가능한 PENDING 상태로 변경합니다.
     *
     * @param outboxEventId 재처리할 Outbox 이벤트 ID
     * @param actorUserId 재처리를 요청한 관리자 사용자 ID
     * @return 재처리 대기 상태로 변경된 Outbox 이벤트
     */
    @Transactional
    public OutboxEventResponse retryFailedEvent(UUID outboxEventId, UUID actorUserId) {
        OutboxEvent outboxEvent = outboxEventRepository.findByOutboxEventIdAndStatus(
                outboxEventId,
                OutboxEventStatus.FAILED
        )
                .orElseThrow(() -> retryTargetNotFound(outboxEventId));

        outboxEvent.resetForRetry(LocalDateTime.now());
        outboxEventAuditService.recordRetryRequested(actorUserId, outboxEvent);
        return OutboxEventResponse.from(outboxEvent);
    }

    private BusinessException retryTargetNotFound(UUID outboxEventId) {
        if (outboxEventRepository.existsById(outboxEventId)) {
            return new BusinessException(
                    CommonErrorCode.BAD_REQUEST,
                    "FAILED 상태의 Outbox 이벤트만 수동 재처리할 수 있습니다."
            );
        }
        return new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND);
    }
}
