package com.san.api.domain.archive.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.san.api.domain.knowledge.entity.CardTag;
import com.san.api.domain.knowledge.entity.KnowledgeCard;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** 아카이브 카테고리 지식카드 응답 DTO */
public record ArchiveCategoryCardResponse(
        UUID cardId,
        String title,

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime createdAt,

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime updatedAt,

        List<ArchiveCardTagResponse> tags
) {

    public static ArchiveCategoryCardResponse from(KnowledgeCard card, List<CardTag> cardTags) {
        return new ArchiveCategoryCardResponse(
                card.getCardId(),
                card.getTitle(),
                card.getCreatedAt(),
                card.getUpdatedAt(),
                cardTags.stream()
                        .map(ArchiveCardTagResponse::from)
                        .toList()
        );
    }
}
