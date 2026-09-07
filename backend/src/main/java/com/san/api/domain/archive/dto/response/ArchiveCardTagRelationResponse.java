package com.san.api.domain.archive.dto.response;

import java.util.List;
import java.util.UUID;

/** 아카이브 카드 태그 연관도 응답 DTO */
public record ArchiveCardTagRelationResponse(
        UUID selectedCardId,
        List<ArchiveRelatedCardResponse> relatedCards
) {
}
