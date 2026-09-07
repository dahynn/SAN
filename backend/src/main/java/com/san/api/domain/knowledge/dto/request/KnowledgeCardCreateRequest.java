package com.san.api.domain.knowledge.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** 지식 카드 생성 요청 DTO */
public record KnowledgeCardCreateRequest(
        @NotNull(message = "scrapId는 필수입니다.")
        UUID scrapId
) {
}
