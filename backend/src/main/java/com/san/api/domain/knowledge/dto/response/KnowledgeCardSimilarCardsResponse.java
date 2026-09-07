package com.san.api.domain.knowledge.dto.response;

import java.util.List;

/** 지식카드 분석 작업 유사 카드 응답 DTO */
public record KnowledgeCardSimilarCardsResponse(
        List<KnowledgeCardResponse> similarCards
) {
}
