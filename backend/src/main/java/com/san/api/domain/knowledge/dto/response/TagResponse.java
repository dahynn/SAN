package com.san.api.domain.knowledge.dto.response;

import java.util.UUID;

/** 태그 응답 DTO */
public record TagResponse(
        UUID tagId,
        String tagName
) {
}
