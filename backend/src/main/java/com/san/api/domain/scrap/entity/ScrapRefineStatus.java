package com.san.api.domain.scrap.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 수집 원본 정제 상태 */
@Getter
@RequiredArgsConstructor
public enum ScrapRefineStatus {
    REFINE_IN_PROGRESS("수집 원본 정제 작업 진행 중"),
    REFINE_COMPLETED("수집 원본 정제 완료");

    private final String description;
}
