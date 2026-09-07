package com.san.api.domain.recall.dto.response;

import com.san.api.domain.recall.entity.RecallQuizType;

import java.time.LocalDate;
import java.util.List;

/** 리콜 퀴즈 생성 응답 DTO */
public record RecallQuizGenerateResponse(
        LocalDate targetDate,
        RecallQuizType quizType,
        List<RecallQuizResponse> quizzes
) {
}
