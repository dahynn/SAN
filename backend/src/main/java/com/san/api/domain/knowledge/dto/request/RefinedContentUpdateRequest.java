package com.san.api.domain.knowledge.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 지식카드 상세보기에서 정제원본 수정 요청 DTO */
public record RefinedContentUpdateRequest(
        @NotBlank(message = "정제원본 내용은 필수입니다.")
        @Size(max = 20000, message = "정제원본 내용은 20000자를 초과할 수 없습니다.")
        String refinedContent
) {
}
