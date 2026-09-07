package com.san.api.domain.knowledge.dto.response;

import com.san.api.domain.knowledge.entity.KnowledgeCard;

import java.util.List;
import java.util.UUID;

/** 통합 검색 응답 DTO */
public record SearchResponse(
        String keyword,
        int page,
        int size,
        long totalCount,
        boolean hasNext,
        List<SearchCardResult> results
) {
    public static SearchResponse of(String keyword, int page, int size,
                                    long totalCount, List<KnowledgeCard> cards) {
        List<SearchCardResult> results = cards.stream()
                .map(SearchCardResult::from)
                .toList();
        boolean hasNext = (long) (page + 1) * size < totalCount;
        return new SearchResponse(keyword, page, size, totalCount, hasNext, results);
    }

    public record SearchCardResult(
            UUID cardId,
            String title,
            String summary
    ) {
        public static SearchCardResult from(KnowledgeCard card) {
            return new SearchCardResult(card.getCardId(), card.getTitle(), card.getSummary());
        }
    }
}
