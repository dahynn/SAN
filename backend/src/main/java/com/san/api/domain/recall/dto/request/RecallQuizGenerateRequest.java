package com.san.api.domain.recall.dto.request;

import com.san.api.domain.recall.entity.RecallQuizType;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/** 리콜 퀴즈 생성 요청 DTO */
public record RecallQuizGenerateRequest(
        @NotNull(message = "리콜 퀴즈 생성 날짜는 필수입니다.")
        LocalDate targetDate,

        @NotNull(message = "리콜 퀴즈 유형은 필수입니다.")
        RecallQuizType quizType
) {
}
