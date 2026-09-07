package com.san.api.domain.archive.dto.response;

import java.util.List;
import java.util.UUID;

/** 아카이브 태그 연관 지식카드 응답 DTO */
public record ArchiveRelatedCardResponse(
        UUID cardId,
        UUID categoryId,
        String categoryName,
        String title,
        long matchedTagCount,
        List<ArchiveCardTagResponse> matchedTags
) {
}
