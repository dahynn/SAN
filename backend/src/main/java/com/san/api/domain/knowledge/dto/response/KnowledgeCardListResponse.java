package com.san.api.domain.knowledge.dto.response;

import java.util.List;

/** 지식 카드 목록 응답 DTO */
public record KnowledgeCardListResponse(
        List<KnowledgeCardResponse> cards
) {
}
