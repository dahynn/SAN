package com.san.api.domain.scrap.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 수집 원본 생성 상태 */
@Getter
@RequiredArgsConstructor
public enum ScrapOriginStatus {
    CREATED("새로 저장된 수집 원본"),
    EXISTING("기존에 저장되어 있던 수집 원본");

    private final String description;
}
