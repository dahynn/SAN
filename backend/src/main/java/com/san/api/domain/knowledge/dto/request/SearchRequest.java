package com.san.api.domain.knowledge.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/** 통합 검색 요청 DTO */
public record SearchRequest(

        @NotBlank
        String keyword,

        String tag,

        String category,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate fromDate,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate toDate,

        @Min(0)
        int page,

        @Min(1) @Max(50)
        int size
) {
    // @ModelAttribute 바인딩 시 미전달 파라미터는 int 기본값 0 → size 기본값 10으로 보정
    public SearchRequest {
        if (size == 0) size = 10;
    }
}
