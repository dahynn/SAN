package com.san.api.domain.knowledge.service;

import com.san.api.domain.knowledge.entity.KnowledgeCard;
import com.san.api.domain.knowledge.repository.KnowledgeCardRepository;
import com.san.api.domain.scrap.repository.ScrapRepository;
import com.san.api.domain.til.entity.DailySummary;
import com.san.api.domain.til.repository.DailySummaryRepository;
import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.KnowledgeErrorCode;
import com.san.api.global.exception.errorcode.TilErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * pgvector 기반 카드 연관 추천 서비스.
 * 카드 기준 유사 카드 추천과 TIL 기준 리콜 카드 추천을 제공한다.
 * 자연어 통합 검색은 HybridSearchService가 담당한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VectorSearchService {

    // pgvector <=> 코사인 거리 기준. 0.5 미만 = 유사도 0.5 초과
    private static final double RECALL_THRESHOLD = 0.5;
    private static final double SIMILAR_CARD_DISTANCE_THRESHOLD = 0.5;

    private final KnowledgeCardRepository knowledgeCardRepository;
    private final ScrapRepository scrapRepository;
    private final DailySummaryRepository dailySummaryRepository;

    /**
     * 카드 기반 연관 카드 추천.
     * 기준 카드의 임베딩을 DB에서 직접 조회하여 AI 서버 호출 없이 검색한다.
     * 결과에서 기준 카드 자신은 제외된다.
     *
     * @param cardId 기준 카드 ID
     * @param userId 요청자 ID (권한 필터)
     * @param limit  최대 반환 개수
     */
    public List<KnowledgeCard> findRelatedByCard(UUID cardId, UUID userId, int limit) {
        KnowledgeCard baseCard = knowledgeCardRepository.findById(cardId)
                .orElseThrow(() -> new BusinessException(KnowledgeErrorCode.CARD_NOT_FOUND));

        if (!baseCard.getScrap().getUser().getUserId().equals(userId)) {
            throw new BusinessException(KnowledgeErrorCode.CARD_ACCESS_DENIED);
        }

        if (baseCard.getEmbedding() == null) {
            return List.of();
        }

        String queryVector = toVectorString(baseCard.getEmbedding());
        List<KnowledgeCard> similarCards = knowledgeCardRepository.searchSimilarCardsByVectorExcludingWithThreshold(
                queryVector, userId, List.of(cardId), SIMILAR_CARD_DISTANCE_THRESHOLD, limit);
        if (!similarCards.isEmpty()) {
            return similarCards;
        }

        return knowledgeCardRepository.searchNearestCardsByVectorExcluding(
                queryVector, userId, List.of(cardId), limit);
    }

    /**
     * TIL 기반 리콜 카드 추천.
     * TIL의 임베딩을 기준으로 유사 카드를 검색하며, 해당 TIL 생성에 사용된 원본 카드는 제외한다.
     * RECALL_THRESHOLD 미만의 코사인 거리를 가진 카드를 전체 반환한다.
     *
     * @param summaryId TIL ID
     * @param userId    요청자 ID (권한 필터)
     */
    public List<KnowledgeCard> findRelatedByTil(UUID summaryId, UUID userId) {
        DailySummary summary = dailySummaryRepository.findById(summaryId)
                .orElseThrow(() -> new BusinessException(TilErrorCode.SUMMARY_NOT_FOUND));

        if (!summary.getUser().getUserId().equals(userId)) {
            throw new BusinessException(TilErrorCode.SUMMARY_ACCESS_DENIED);
        }

        if (summary.getEmbedding() == null) {
            return List.of();
        }

        String queryVector = toVectorString(summary.getEmbedding());
        List<UUID> excludeIds = scrapRepository.findCardIdsByUserAndDate(userId, summary.getTargetDate());

        // NOT IN () 은 SQL 오류 유발 — 원본 카드가 없으면 리콜 대상도 없음
        if (excludeIds.isEmpty()) {
            return List.of();
        }

        List<KnowledgeCard> relatedCards = knowledgeCardRepository.searchByVectorExcludingWithThreshold(
                queryVector, userId, excludeIds, RECALL_THRESHOLD);
        if (!relatedCards.isEmpty()) {
            return relatedCards;
        }

        return knowledgeCardRepository.searchNearestCardsByVectorExcluding(
                queryVector, userId, excludeIds);
    }

    private String toVectorString(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            sb.append(vector[i]);
            if (i < vector.length - 1) sb.append(",");
        }
        return sb.append("]").toString();
    }
}
