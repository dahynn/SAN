package com.san.api.domain.archive.dto.response;

import java.util.List;
import java.util.UUID;

/** 아카이브 카테고리 지식카드 목록 응답 DTO */
public record ArchiveCategoryCardListResponse(
        UUID categoryId,
        String categoryName,
        List<ArchiveCategoryCardResponse> cards
) {
}
