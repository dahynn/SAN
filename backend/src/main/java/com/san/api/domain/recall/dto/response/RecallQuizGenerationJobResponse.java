package com.san.api.domain.recall.dto.response;

import com.san.api.domain.recall.entity.RecallQuizType;

import java.time.LocalDate;
import java.util.UUID;

/** 리콜 퀴즈 생성 작업 등록 응답 DTO */
public record RecallQuizGenerationJobResponse(
        UUID generationId,
        UUID quizJobId,
        LocalDate targetDate,
        RecallQuizType quizType
) {
}
