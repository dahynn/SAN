package com.san.api.domain.archive.dto.response;

import java.util.List;

/** 아카이브 카테고리 목록 응답 DTO */
public record ArchiveCategoryListResponse(
        List<ArchiveCategoryResponse> categories
) {
}
