package com.san.api.domain.recall.service;

import com.san.api.domain.knowledge.entity.KnowledgeCard;
import com.san.api.domain.knowledge.repository.KnowledgeCardRepository;
import com.san.api.domain.til.entity.DailySummary;
import com.san.api.domain.til.repository.DailySummaryRepository;
import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.RecallErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** 리콜 퀴즈 대상 원본 조회 Service */
@Service
@RequiredArgsConstructor
public class RecallQuizSourceService {

    private final DailySummaryRepository dailySummaryRepository;
    private final KnowledgeCardRepository knowledgeCardRepository;

    /**
     * 날짜 기반 리콜 퀴즈 대상 원본 조회
     *
     * @param userId 사용자 ID
     * @param targetDate 대상 날짜
     * @return 리콜 퀴즈 대상 원본
     */
    @Transactional(readOnly = true)
    public RecallQuizSourceResult findSources(UUID userId, LocalDate targetDate) {
        DailySummary summary = findLatestSummary(userId, targetDate);
        List<KnowledgeCard> sourceCards = knowledgeCardRepository.findTilSourceCards(
                userId,
                targetDate.atStartOfDay(),
                targetDate.plusDays(1).atStartOfDay()
        );

        if (sourceCards.isEmpty()) {
            throw new BusinessException(RecallErrorCode.EMPTY_RECALL_SOURCE);
        }

        return new RecallQuizSourceResult(summary, sourceCards);
    }

    /** 최신 TIL 조회 */
    private DailySummary findLatestSummary(UUID userId, LocalDate targetDate) {
        return dailySummaryRepository.findAllByUserIdAndTargetDateWithUserOrderByCreatedAtDesc(userId, targetDate)
                .stream()
                .findFirst()
                .orElseThrow(() -> new BusinessException(RecallErrorCode.RECALL_TIL_NOT_FOUND));
    }

    /** 퀴즈 저장 기준 TIL과 AI 요청 대상 원본 전달 */
    public record RecallQuizSourceResult(
            DailySummary dailySummary,
            List<KnowledgeCard> sourceCards
    ) {
    }
}
