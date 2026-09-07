package com.san.api.global.outbox.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Outbox를 통해 비동기로 처리할 이벤트 유형을 정의합니다. */
@Getter
@RequiredArgsConstructor
public enum OutboxEventType {
    /** 피드백 등록 사실을 Mattermost 웹훅으로 알리는 이벤트입니다. */
    FEEDBACK_MATTERMOST_NOTIFICATION("피드백 Mattermost 알림 전송");

    private final String description;
}
