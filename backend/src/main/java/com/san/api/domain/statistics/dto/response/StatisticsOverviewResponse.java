package com.san.api.domain.statistics.dto.response;

/**
 * 대시보드 통계 요약 응답.
 *
 * @param totalKnowledgeCardCount 전체 지식카드 개수
 * @param todayKnowledgeCardCount 오늘 생성한 지식카드 개수
 * @param totalTilCount 전체 TIL 개수
 */
public record StatisticsOverviewResponse(
        long totalKnowledgeCardCount,
        long todayKnowledgeCardCount,
        long totalTilCount
) {
}
