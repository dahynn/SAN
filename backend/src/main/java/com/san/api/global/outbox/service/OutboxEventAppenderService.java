package com.san.api.global.outbox.service;

import com.san.api.global.outbox.entity.OutboxEvent;
import com.san.api.global.outbox.entity.OutboxEventType;
import com.san.api.global.outbox.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

/** 비즈니스 트랜잭션 안에서 Outbox 이벤트를 생성하는 서비스입니다. */
@Service
@RequiredArgsConstructor
public class OutboxEventAppenderService {

    private static final int DEFAULT_MAX_RETRY_COUNT = 3;

    private final OutboxEventRepository outboxEventRepository;

    /**
     * 처리 대기 상태의 Outbox 이벤트를 저장합니다.
     *
     * @param eventType     처리할 이벤트 유형
     * @param aggregateType 이벤트가 발생한 원본 도메인 유형
     * @param aggregateId   이벤트가 발생한 원본 도메인 식별자
     * @param payload       외부 전송에 필요한 데이터 스냅샷
     * @return 저장된 Outbox 이벤트
     */
    public OutboxEvent append(
            OutboxEventType eventType,
            String aggregateType,
            UUID aggregateId,
            Map<String, Object> payload
    ) {
        OutboxEvent event = OutboxEvent.builder()
                .eventType(eventType)
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .payload(payload)
                .maxRetryCount(DEFAULT_MAX_RETRY_COUNT)
                .build();

        return outboxEventRepository.save(event);
    }
}
