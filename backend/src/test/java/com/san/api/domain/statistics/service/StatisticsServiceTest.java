package com.san.api.domain.statistics.service;

import com.san.api.domain.knowledge.repository.KnowledgeCardRepository;
import com.san.api.domain.statistics.dto.response.StatisticsOverviewResponse;
import com.san.api.domain.til.repository.DailySummaryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/** 사용자 통계 조회 Service 테스트 */
@ExtendWith(MockitoExtension.class)
class StatisticsServiceTest {

    @Mock
    private KnowledgeCardRepository knowledgeCardRepository;
    @Mock
    private DailySummaryRepository dailySummaryRepository;

    /** 대시보드 통계 요약 응답에 지식카드와 TIL 개수가 포함되는지 검증 */
    @Test
    void getOverviewReturnsDashboardCounts() {
        UUID userId = UUID.randomUUID();
        StatisticsService statisticsService = new StatisticsService(
                knowledgeCardRepository,
                dailySummaryRepository
        );
        LocalDate today = LocalDate.now();
        LocalDateTime startAt = today.atStartOfDay();
        LocalDateTime endAt = today.plusDays(1).atStartOfDay();

        when(knowledgeCardRepository.countByScrap_User_UserId(userId)).thenReturn(38L);
        when(knowledgeCardRepository.countByScrap_User_UserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                userId,
                startAt,
                endAt
        )).thenReturn(3L);
        when(dailySummaryRepository.countByUser_UserId(userId)).thenReturn(12L);

        StatisticsOverviewResponse response = statisticsService.getOverview(userId);

        assertThat(response.totalKnowledgeCardCount()).isEqualTo(38L);
        assertThat(response.todayKnowledgeCardCount()).isEqualTo(3L);
        assertThat(response.totalTilCount()).isEqualTo(12L);
    }
}
