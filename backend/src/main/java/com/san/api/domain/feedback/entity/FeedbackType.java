package com.san.api.domain.feedback.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 사용자가 등록하는 서비스 피드백 유형을 구분합니다. */
@Getter
@RequiredArgsConstructor
public enum FeedbackType {
    BUG("버그 제보"),
    INCONVENIENCE("서비스 이용 중 불편한 점"),
    FEATURE_REQUEST("추가되면 좋은 기능 제안"),
    ETC("기타 의견");

    private final String description;
}
