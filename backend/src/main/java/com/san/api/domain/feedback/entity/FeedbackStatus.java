package com.san.api.domain.feedback.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 등록된 피드백의 처리 상태를 구분합니다. */
@Getter
@RequiredArgsConstructor
public enum FeedbackStatus {
    NEW("새로 등록된 피드백"),
    REVIEWED("확인 완료된 피드백"),
    DONE("처리 완료된 피드백");

    private final String description;
}
