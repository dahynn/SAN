package com.san.api.domain.scrap.dto.request;

/** 스크랩 생성 요청 DTO */
public record ScrapCreateRequest(
        String sourceUrl,
        String rawContent,
        String imageObjectKey
) {
}
