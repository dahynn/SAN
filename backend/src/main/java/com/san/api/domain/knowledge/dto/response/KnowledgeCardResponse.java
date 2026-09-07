package com.san.api.domain.knowledge.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.san.api.domain.knowledge.entity.CardTag;
import com.san.api.domain.knowledge.entity.KnowledgeCard;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** 지식카드 응답 DTO */
public record KnowledgeCardResponse(
        UUID cardId,
        String title,
        String summary,
        CategoryResponse category,
        List<TagResponse> tags,

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime createdAt
) {
    public static KnowledgeCardResponse from(KnowledgeCard card, List<CardTag> cardTags) {
        return new KnowledgeCardResponse(
                card.getCardId(),
                card.getTitle(),
                card.getSummary(),
                new CategoryResponse(
                        card.getCategory().getCategoryId(),
                        card.getCategory().getCategoryName()
                ),
                cardTags.stream()
                        .map(cardTag -> new TagResponse(
                                cardTag.getTag().getTagId(),
                                cardTag.getTag().getTagName()
                        ))
                        .toList(),
                card.getCreatedAt()
        );
    }
}
