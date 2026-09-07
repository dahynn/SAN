package com.san.api.domain.knowledge.service;

import com.san.api.domain.knowledge.dto.response.SearchResponse;
import com.san.api.domain.knowledge.entity.KnowledgeCard;
import com.san.api.domain.knowledge.repository.KnowledgeCardRepository;
import com.san.api.global.external.ai.client.AiEmbeddingClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Stream;

/**
 * 벡터 검색과 키워드 검색을 병합하는 Hybrid 검색 서비스.
 *
 * 병합 우선순위:
 *   1) 두 검색 모두 히트 — 벡터 유사도 순
 *   2) 벡터 검색만 히트 — 벡터 유사도 순
 *   3) 키워드 검색만 히트 — created_at DESC
 *
 * 각 검색은 최대 HYBRID_FETCH_LIMIT개까지 조회하며, 병합·페이지네이션은 서비스 레이어에서 처리한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HybridSearchService {

    private static final double RECALL_THRESHOLD = 0.3;
    private static final int HYBRID_FETCH_LIMIT = 200;

    private final KnowledgeCardRepository knowledgeCardRepository;
    private final AiEmbeddingClient aiEmbeddingClient;

    /**
     * 키워드를 벡터 검색과 ILIKE 검색으로 동시에 조회하고 결과를 병합하여 반환한다.
     * tag, category, fromDate, toDate는 null 전달 시 필터 미적용.
     */
    public SearchResponse search(String keyword, UUID userId, String tag, String category,
                                 LocalDate fromDate, LocalDate toDate, int page, int size) {
        // 현재 페이지의 hasNext 판단에 필요한 최소 후보 수를 보장
        int fetchLimit = Math.max(HYBRID_FETCH_LIMIT, (page + 1) * size + 1);

        // 벡터 검색
        float[] vector = aiEmbeddingClient.embed(keyword);
        String queryVector = toVectorString(vector);
        List<KnowledgeCard> vectorCards = knowledgeCardRepository.searchByVectorWithFilters(
                queryVector, userId, tag, category, fromDate, toDate,
                RECALL_THRESHOLD, fetchLimit, 0);

        // 키워드 검색
        String pattern = "%" + keyword + "%";
        List<KnowledgeCard> keywordCards = knowledgeCardRepository.searchByKeyword(
                pattern, userId, tag, category, fromDate, toDate, fetchLimit);

        // 병합
        List<KnowledgeCard> merged = merge(vectorCards, keywordCards);

        // 페이지네이션
        long totalCount = merged.size();
        List<KnowledgeCard> paged = merged.stream()
                .skip((long) page * size)
                .limit(size)
                .toList();

        return SearchResponse.of(keyword, page, size, totalCount, paged);
    }

    /**
     * 두 결과를 우선순위(both > vector-only > keyword-only)로 정렬하고 중복을 제거한다.
     * vectorCards는 이미 유사도 순, keywordCards는 이미 created_at DESC 순이므로 순서를 유지한다.
     */
    private List<KnowledgeCard> merge(List<KnowledgeCard> vectorCards, List<KnowledgeCard> keywordCards) {
        Set<UUID> vectorIds = new LinkedHashSet<>();
        vectorCards.forEach(c -> vectorIds.add(c.getCardId()));

        Set<UUID> keywordIds = new LinkedHashSet<>();
        keywordCards.forEach(c -> keywordIds.add(c.getCardId()));

        List<KnowledgeCard> bothHit = vectorCards.stream()
                .filter(c -> keywordIds.contains(c.getCardId()))
                .toList();

        List<KnowledgeCard> vectorOnly = vectorCards.stream()
                .filter(c -> !keywordIds.contains(c.getCardId()))
                .toList();

        List<KnowledgeCard> keywordOnly = keywordCards.stream()
                .filter(c -> !vectorIds.contains(c.getCardId()))
                .toList();

        return Stream.of(bothHit, vectorOnly, keywordOnly)
                .flatMap(List::stream)
                .toList();
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