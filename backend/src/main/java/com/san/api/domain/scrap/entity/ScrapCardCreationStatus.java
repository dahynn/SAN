package com.san.api.domain.scrap.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 지식카드 생성 상태 */
@Getter
@RequiredArgsConstructor
public enum ScrapCardCreationStatus {
    ANALYSIS_IN_PROGRESS("지식카드 분석 작업 진행 중"),
    CARD_READY("지식카드 생성 완료");

    private final String description;
}
