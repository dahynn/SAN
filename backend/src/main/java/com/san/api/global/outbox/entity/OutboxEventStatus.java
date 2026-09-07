package com.san.api.global.outbox.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Outbox 이벤트의 처리 상태를 나타냅니다. */
@Getter
@RequiredArgsConstructor
public enum OutboxEventStatus {
    /** 아직 처리되지 않았거나 재시도를 기다리는 상태입니다. */
    PENDING("처리 대기"),

    /** 릴레이 또는 스케줄러가 이벤트를 처리 중인 상태입니다. */
    PROCESSING("처리 중"),

    /** 외부 전송 또는 후속 처리가 성공적으로 완료된 상태입니다. */
    SENT("처리 완료"),

    /** 최대 재시도 횟수를 초과했거나 운영자 확인이 필요한 실패 상태입니다. */
    FAILED("처리 실패");

    private final String description;
}
