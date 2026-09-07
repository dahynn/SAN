package com.san.api.domain.til.dto.response;

import java.time.LocalDate;
import java.util.UUID;

/** TIL 생성 작업 등록 응답 DTO */
public record TilGenerationJobResponse(
        UUID summaryId,
        UUID jobId,
        LocalDate targetDate
) {
}
