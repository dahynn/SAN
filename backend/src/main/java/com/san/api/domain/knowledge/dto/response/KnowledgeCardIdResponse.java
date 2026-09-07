package com.san.api.domain.knowledge.dto.response;

import java.util.UUID;

/** 수집 원본 기준 생성된 지식카드 ID 응답 DTO */
public record KnowledgeCardIdResponse(
        UUID scrapId,
        UUID cardId
) {
    /** 수집 원본 ID와 지식카드 ID 기반 응답 생성 */
    public static KnowledgeCardIdResponse from(UUID scrapId, UUID cardId) {
        return new KnowledgeCardIdResponse(scrapId, cardId);
    }
}
