package com.san.api.domain.statistics.service;

import com.san.api.domain.knowledge.repository.KnowledgeCardRepository;
import com.san.api.domain.statistics.dto.response.StatisticsOverviewResponse;
import com.san.api.domain.til.repository.DailySummaryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/** 사용자 통계 조회 Service */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatisticsService {

    private final KnowledgeCardRepository knowledgeCardRepository;
    private final DailySummaryRepository dailySummaryRepository;

    /**
     * 대시보드 숫자 카드에 사용할 통계 요약을 조회합니다.
     *
     * @param userId 사용자 ID
     * @return 전체 지식카드 수, 오늘 생성한 지식카드 수, 전체 TIL 수
     */
    public StatisticsOverviewResponse getOverview(UUID userId) {
        LocalDate today = LocalDate.now();
        LocalDateTime startAt = today.atStartOfDay();
        LocalDateTime endAt = today.plusDays(1).atStartOfDay();

        long totalKnowledgeCardCount = knowledgeCardRepository.countByScrap_User_UserId(userId);
        long todayKnowledgeCardCount = knowledgeCardRepository
                .countByScrap_User_UserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(userId, startAt, endAt);
        long totalTilCount = dailySummaryRepository.countByUser_UserId(userId);

        return new StatisticsOverviewResponse(
                totalKnowledgeCardCount,
                todayKnowledgeCardCount,
                totalTilCount
        );
    }
}
