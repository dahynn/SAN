package com.san.api.domain.til.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/** TIL 생성 작업 등록 요청 DTO */
public record TilGenerateRequest(
        @NotNull(message = "TIL 생성 대상 날짜는 필수입니다.")
        LocalDate targetDate
) {
}
