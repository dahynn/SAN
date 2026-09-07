package com.san.api.domain.archive.dto.response;

import com.san.api.domain.knowledge.repository.CategoryRepository;

import java.util.UUID;

/** 아카이브 카테고리 응답 DTO */
public record ArchiveCategoryResponse(
        UUID categoryId,
        String categoryName,
        long cardCount
) {

    public static ArchiveCategoryResponse from(CategoryRepository.CategoryCardCountProjection projection) {
        return new ArchiveCategoryResponse(
                projection.getCategoryId(),
                projection.getCategoryName(),
                projection.getCardCount()
        );
    }
}
