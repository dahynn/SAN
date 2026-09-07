package com.san.api.domain.til.dto.response;

import java.time.LocalDate;
import java.util.List;

/** TIL 생성용 지식 원본 목록 응답 DTO */
public record TilGenerationSourceResponse(
        LocalDate targetDate,
        List<TilGenerationSourceContentResponse> contents
) {
}
