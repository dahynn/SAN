package com.san.api.domain.knowledge.dto.response;

import com.san.api.domain.knowledge.entity.CardTag;
import com.san.api.domain.knowledge.entity.KnowledgeCard;
import com.san.api.domain.scrap.entity.SourceType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** 지식카드 상세 조회 응답 DTO */
public record KnowledgeCardDetailResponse(
        String title,
        UUID categoryId,
        String categoryName,
        SourceType sourceType,
        String sourceContent,
        String refinedContent,
        String summary,
        List<String> tags,
        LocalDateTime collectedAt
) {

    public static KnowledgeCardDetailResponse from(
            KnowledgeCard card,
            List<CardTag> cardTags,
            String sourceContent
    ) {
        return new KnowledgeCardDetailResponse(
                card.getTitle(),
                card.getCategory().getCategoryId(),
                card.getCategory().getCategoryName(),
                card.getScrap().getSourceType(),
                sourceContent,
                card.getScrap().getRefinedContent(),
                card.getSummary(),
                cardTags.stream()
                        .map(cardTag -> cardTag.getTag().getTagName())
                        .toList(),
                card.getScrap().getCreatedAt()
        );
    }
}
