package com.san.api.domain.scrap.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 스크랩 원본 데이터 유형 */
@Getter
@RequiredArgsConstructor
public enum SourceType {
    LINK("링크"),
    TEXT("텍스트"),
    IMAGE("이미지");

    private final String description;
}
