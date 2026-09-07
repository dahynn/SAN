package com.san.api.domain.til.dto.response;

import com.san.api.domain.til.entity.DailySummary;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/** 날짜 기준 TIL 조회 응답 DTO */
public record TilResponse(
        UUID summaryId,
        LocalDate targetDate,
        String title,
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    /**
     * 매일의 요약 엔티티를 TIL 조회 응답으로 변환
     *
     * @param summary 매일의 요약 엔티티
     * @return TIL 조회 응답 DTO
     */
    public static TilResponse from(DailySummary summary) {
        return new TilResponse(
                summary.getSummaryId(),
                summary.getTargetDate(),
                summary.getTitle(),
                summary.getContent(),
                summary.getCreatedAt(),
                summary.getUpdatedAt()
        );
    }
}
