package com.san.api.domain.til.dto.response;

import com.san.api.domain.knowledge.dto.response.KnowledgeCardResponse;

import java.util.List;

/** TIL 기반 리콜 카드 응답 DTO */
public record TilRecallCardsResponse(
        List<KnowledgeCardResponse> recallCards
) {
}
