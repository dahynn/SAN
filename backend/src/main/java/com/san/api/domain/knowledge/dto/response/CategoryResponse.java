package com.san.api.domain.knowledge.dto.response;

import java.util.UUID;

/** 카테고리 응답 DTO */
public record CategoryResponse(
        UUID categoryId,
        String categoryName
) {
}
