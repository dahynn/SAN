package com.san.api.domain.archive.service;

import com.san.api.domain.archive.dto.response.ArchiveCardTagRelationResponse;
import com.san.api.domain.archive.dto.response.ArchiveCardTagResponse;
import com.san.api.domain.archive.dto.response.ArchiveCategoryCardListResponse;
import com.san.api.domain.archive.dto.response.ArchiveCategoryCardResponse;
import com.san.api.domain.archive.dto.response.ArchiveCategoryListResponse;
import com.san.api.domain.archive.dto.response.ArchiveCategoryResponse;
import com.san.api.domain.archive.dto.response.ArchiveRelatedCardResponse;
import com.san.api.domain.knowledge.entity.CardTag;
import com.san.api.domain.knowledge.entity.Category;
import com.san.api.domain.knowledge.entity.KnowledgeCard;
import com.san.api.domain.knowledge.repository.CardTagRepository;
import com.san.api.domain.knowledge.repository.CardTagRepository.CardTagRelationProjection;
import com.san.api.domain.knowledge.repository.CategoryRepository;
import com.san.api.domain.knowledge.repository.KnowledgeCardRepository;
import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.CommonErrorCode;
import com.san.api.global.exception.errorcode.KnowledgeErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/** 아카이브 조회 Service */
@Service
@RequiredArgsConstructor
public class ArchiveService {

    private final CategoryRepository categoryRepository;
    private final KnowledgeCardRepository knowledgeCardRepository;
    private final CardTagRepository cardTagRepository;

    /**
     * 아카이브 카테고리 목록과 카테고리별 지식카드 개수 조회
     *
     * @param userId 로그인 사용자 ID
     * @return 아카이브 카테고리 목록 응답
     */
    @Transactional(readOnly = true)
    public ArchiveCategoryListResponse getCategories(UUID userId) {
        return new ArchiveCategoryListResponse(
                categoryRepository.findArchiveCategoryCounts(userId)
                        .stream()
                        .map(ArchiveCategoryResponse::from)
                        .toList()
        );
    }

    /**
     * 아카이브 카테고리별 지식카드 목록 조회
     *
     * @param userId 로그인 사용자 ID
     * @param categoryId 카테고리 ID
     * @return 카테고리별 지식카드 목록 응답
     */
    @Transactional(readOnly = true)
    public ArchiveCategoryCardListResponse getCategoryCards(UUID userId, UUID categoryId) {
        // 사용자 소유 카테고리만 조회해 타 사용자 카테고리 접근을 차단
        Category category = categoryRepository.findByCategoryIdAndUser_UserId(categoryId, userId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND));

        List<KnowledgeCard> cards = knowledgeCardRepository.findArchiveCardsByCategory(userId, categoryId);
        if (cards.isEmpty()) {
            // 프론트가 선택한 카테고리 정보를 유지할 수 있도록 빈 목록과 함께 반환
            return new ArchiveCategoryCardListResponse(
                    category.getCategoryId(),
                    category.getCategoryName(),
                    List.of()
            );
        }

        // 카드별 태그 목록을 한 번에 조회한 뒤 cardId 기준으로 묶어 N+1 조회를 방지
        Map<UUID, List<CardTag>> cardTagsByCardId = cardTagRepository.findAllByKnowledgeCardInWithTag(cards).stream()
                .collect(Collectors.groupingBy(cardTag -> cardTag.getKnowledgeCard().getCardId()));

        return new ArchiveCategoryCardListResponse(
                category.getCategoryId(),
                category.getCategoryName(),
                cards.stream()
                        .map(card -> ArchiveCategoryCardResponse.from(
                                card,
                                cardTagsByCardId.getOrDefault(card.getCardId(), List.of())
                        ))
                        .toList()
        );
    }

    /**
     * 아카이브 카드 태그 연관도 조회
     *
     * @param userId 로그인 사용자 ID
     * @param cardId 선택 지식카드 ID
     * @return 선택 카드와 태그가 겹치는 지식카드 목록 응답
     */
    @Transactional(readOnly = true)
    public ArchiveCardTagRelationResponse getCardTagRelations(UUID userId, UUID cardId) {
        KnowledgeCard selectedCard = knowledgeCardRepository.findByCardIdWithScrapAndCategory(cardId)
                .orElseThrow(() -> new BusinessException(KnowledgeErrorCode.CARD_NOT_FOUND));

        if (!selectedCard.getScrap().getUser().getUserId().equals(userId)) {
            throw new BusinessException(KnowledgeErrorCode.CARD_ACCESS_DENIED);
        }

        List<UUID> tagIds = cardTagRepository.findAllByCardIdWithTag(cardId).stream()
                .map(cardTag -> cardTag.getTag().getTagId())
                .toList();
        if (tagIds.isEmpty()) {
            // 태그가 없는 카드는 연관도를 계산할 기준이 없으므로 빈 목록 반환
            return new ArchiveCardTagRelationResponse(cardId, List.of());
        }

        // Projection은 매칭 태그 단위 row이므로 카드별로 묶어 relatedCards 응답을 만든다
        Map<UUID, List<CardTagRelationProjection>> relationsByCardId = cardTagRepository
                .findRelatedCardTagRelations(userId, cardId, tagIds)
                .stream()
                .collect(Collectors.groupingBy(
                        CardTagRelationProjection::getCardId,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        List<ArchiveRelatedCardResponse> relatedCards = relationsByCardId.values().stream()
                .map(this::toRelatedCardResponse)
                .toList();

        return new ArchiveCardTagRelationResponse(cardId, relatedCards);
    }

    private ArchiveRelatedCardResponse toRelatedCardResponse(List<CardTagRelationProjection> relations) {
        CardTagRelationProjection first = relations.get(0);

        return new ArchiveRelatedCardResponse(
                first.getCardId(),
                first.getCategoryId(),
                first.getCategoryName(),
                first.getTitle(),
                first.getMatchedTagCount(),
                relations.stream()
                        .map(relation -> new ArchiveCardTagResponse(
                                relation.getTagId(),
                                relation.getTagName()
                        ))
                        .toList()
        );
    }
}
