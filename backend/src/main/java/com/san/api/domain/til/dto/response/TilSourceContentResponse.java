package com.san.api.domain.til.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.san.api.domain.knowledge.dto.response.CategoryResponse;
import com.san.api.domain.scrap.entity.SourceType;

import java.time.LocalDateTime;
import java.util.UUID;

/** TIL 생성 원본 단건 응답 DTO */
public record TilSourceContentResponse(
        UUID cardId,
        UUID scrapId,
        String title,
        SourceType sourceType,
        String rawContent,
        String sourceUrl,
        String imageUrl,
        CategoryResponse category,

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime createdAt
) {
}
