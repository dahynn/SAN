package com.san.api.domain.knowledge.service;

import com.san.api.domain.knowledge.dto.request.KnowledgeCardCreateRequest;
import com.san.api.domain.knowledge.dto.request.RefinedContentUpdateRequest;
import com.san.api.domain.knowledge.dto.response.KnowledgeCardAnalysisJobResponse;
import com.san.api.domain.knowledge.dto.response.KnowledgeCardDetailResponse;
import com.san.api.domain.knowledge.dto.response.KnowledgeCardIdResponse;
import com.san.api.domain.knowledge.dto.response.KnowledgeCardListResponse;
import com.san.api.domain.knowledge.dto.response.KnowledgeCardResponse;
import com.san.api.domain.knowledge.dto.response.KnowledgeCardSimilarCardsResponse;
import com.san.api.domain.knowledge.entity.CardTag;
import com.san.api.domain.knowledge.entity.KnowledgeCard;
import com.san.api.domain.knowledge.repository.CardTagRepository;
import com.san.api.domain.knowledge.repository.KnowledgeCardRepository;
import com.san.api.domain.scrap.entity.Scrap;
import com.san.api.domain.scrap.entity.SourceType;
import com.san.api.domain.scrap.repository.ScrapRepository;
import com.san.api.global.async.entity.JobType;
import com.san.api.global.async.service.AsyncJobManager;
import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.CommonErrorCode;
import com.san.api.global.exception.errorcode.KnowledgeErrorCode;
import com.san.api.global.exception.errorcode.ScrapErrorCode;
import com.san.api.global.external.s3.service.S3PresignedUrlService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/** 지식카드 Service */
@Service
@RequiredArgsConstructor
public class KnowledgeCardService {

    private static final int RELATED_CARD_LIMIT = 3;

    private final ScrapRepository scrapRepository;
    private final KnowledgeCardRepository knowledgeCardRepository;
    private final CardTagRepository cardTagRepository;
    private final AsyncJobManager asyncJobManager;
    private final VectorSearchService vectorSearchService;
    private final S3PresignedUrlService s3PresignedUrlService;

    /**
     * 저장된 수집 원본 기반 지식카드 AI 분석 작업 등록
     *
     * @param userId 로그인 사용자 ID
     * @param request 지식카드 AI 분석 요청
     * @return 등록된 비동기 작업 응답
     */
    @Transactional
    public KnowledgeCardAnalysisJobResponse createCard(UUID userId, KnowledgeCardCreateRequest request) {
        Scrap scrap = scrapRepository.findById(request.scrapId())
                .orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND));

        validateScrapOwner(scrap, userId);
        validateNotCreated(request.scrapId());

        UUID jobId = asyncJobManager.enqueue(JobType.CARD_ANALYSIS, request.scrapId());
        return new KnowledgeCardAnalysisJobResponse(jobId);
    }

    /**
     * 로그인 사용자 기준 지식카드 목록 조회
     *
     * @param userId 로그인 사용자 ID
     * @return 지식카드 목록 응답
     */
    @Transactional(readOnly = true)
    public KnowledgeCardListResponse getCards(UUID userId) {
        List<KnowledgeCard> cards = knowledgeCardRepository.findByScrap_User_UserIdOrderByCreatedAtDesc(userId);
        if (cards.isEmpty()) {
            return new KnowledgeCardListResponse(List.of());
        }

        return new KnowledgeCardListResponse(toKnowledgeCardResponses(cards));
    }

    /**
     * 수집 원본 기준 생성된 지식카드 ID 조회
     *
     * @param userId 로그인 사용자 ID
     * @param scrapId 수집 원본 ID
     * @return 생성된 지식카드 ID 응답
     */
    @Transactional(readOnly = true)
    public KnowledgeCardIdResponse getCardIdByScrap(UUID userId, UUID scrapId) {
        Scrap scrap = scrapRepository.findById(scrapId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND));
        validateScrapOwner(scrap, userId);

        KnowledgeCard card = getCreatedCard(scrapId);
        return KnowledgeCardIdResponse.from(scrapId, card.getCardId());
    }

    /**
     * 지식카드 기준 유사 카드 조회
     *
     * @param userId 로그인 사용자 ID
     * @param cardId 지식카드 ID
     * @return 유사 지식카드 목록 응답
     */
    @Transactional(readOnly = true)
    public KnowledgeCardSimilarCardsResponse getSimilarCardsByCard(UUID userId, UUID cardId) {
        List<KnowledgeCard> similarCards = vectorSearchService.findRelatedByCard(
                cardId,
                userId,
                RELATED_CARD_LIMIT
        );

        return new KnowledgeCardSimilarCardsResponse(
                toKnowledgeCardResponses(similarCards)
        );
    }

    /**
     * 지식카드 상세 조회
     *
     * @param userId 로그인 사용자 ID
     * @param cardId 지식카드 ID
     * @return 지식카드 상세 조회 응답
     */
    @Transactional(readOnly = true)
    public KnowledgeCardDetailResponse getCardDetail(UUID userId, UUID cardId) {
        KnowledgeCard card = knowledgeCardRepository.findByCardIdWithScrapAndCategory(cardId)
                .orElseThrow(() -> new BusinessException(KnowledgeErrorCode.CARD_NOT_FOUND));
        validateCardOwner(card, userId);

        List<CardTag> cardTags = cardTagRepository.findAllByKnowledgeCardInWithTag(List.of(card));
        return KnowledgeCardDetailResponse.from(card, cardTags, createSourceContent(card.getScrap()));
    }

    /**
     * 지식카드 상세보기에서 정제원본 수정
     *
     * @param userId 로그인 사용자 ID
     * @param cardId 지식카드 ID
     * @param request 정제원본 수정 요청
     */
    @Transactional
    public KnowledgeCardDetailResponse updateRefinedContent(
            UUID userId,
            UUID cardId,
            RefinedContentUpdateRequest request
    ) {
        KnowledgeCard card = knowledgeCardRepository.findByCardIdWithScrapAndCategory(cardId)
                .orElseThrow(() -> new BusinessException(KnowledgeErrorCode.CARD_NOT_FOUND));
        validateCardOwner(card, userId);

        card.getScrap().updateRefinedContent(request.refinedContent().trim());

        List<CardTag> cardTags = cardTagRepository.findAllByKnowledgeCardInWithTag(List.of(card));
        return KnowledgeCardDetailResponse.from(card, cardTags, createSourceContent(card.getScrap()));
    }

    /**
     * 지식카드 삭제
     *
     * @param userId 로그인 사용자 ID
     * @param cardId 지식카드 ID
     */
    @Transactional
    public void deleteCard(UUID userId, UUID cardId) {
        KnowledgeCard card = knowledgeCardRepository.findByCardIdWithScrapAndCategory(cardId)
                .orElseThrow(() -> new BusinessException(KnowledgeErrorCode.CARD_NOT_FOUND));
        validateCardOwner(card, userId);

        card.deleteCard();
    }

    /** 수집 원본 기준 생성된 지식카드 조회 */
    private KnowledgeCard getCreatedCard(UUID scrapId) {
        return knowledgeCardRepository.findByScrapIdWithCategory(scrapId)
                .orElseThrow(() -> new BusinessException(KnowledgeErrorCode.CARD_NOT_FOUND));
    }

    /** 지식카드 목록 응답 변환 */
    private List<KnowledgeCardResponse> toKnowledgeCardResponses(List<KnowledgeCard> cards) {
        if (cards.isEmpty()) {
            return List.of();
        }

        Map<UUID, List<CardTag>> cardTagsByCardId = cardTagRepository.findAllByKnowledgeCardInWithTag(cards).stream()
                .collect(Collectors.groupingBy(cardTag -> cardTag.getKnowledgeCard().getCardId()));

        return cards.stream()
                .map(card -> KnowledgeCardResponse.from(
                        card,
                        cardTagsByCardId.getOrDefault(card.getCardId(), List.of())
                ))
                .toList();
    }

    /**
     * 원본 소유자 검증
     *
     * @param scrap 수집 원본
     * @param userId 로그인 사용자 ID
     */
    private void validateScrapOwner(Scrap scrap, UUID userId) {
        if (!scrap.getUser().getUserId().equals(userId)) {
            throw new BusinessException(ScrapErrorCode.SCRAP_ACCESS_DENIED);
        }
    }

    /** 지식카드 소유자 검증 */
    private void validateCardOwner(KnowledgeCard card, UUID userId) {
        if (!card.getScrap().getUser().getUserId().equals(userId)) {
            throw new BusinessException(KnowledgeErrorCode.CARD_ACCESS_DENIED);
        }
    }

    /**
     * 동일 원본 기반 지식카드 생성 여부 검증
     *
     * @param scrapId 수집 원본 ID
     */
    private void validateNotCreated(UUID scrapId) {
        if (knowledgeCardRepository.existsByScrap_ScrapId(scrapId)) {
            throw new BusinessException(KnowledgeErrorCode.CARD_ALREADY_EXISTS);
        }
    }

    private String createSourceContent(Scrap scrap) {
        if (scrap.getSourceType() == SourceType.IMAGE) {
            return createImageUrl(scrap.getImageObjectKey());
        }

        if (scrap.getSourceType() == SourceType.LINK) {
            return scrap.getSourceUrl();
        }

        return scrap.getRawContent();
    }

    private String createImageUrl(String imageObjectKey) {
        if (imageObjectKey == null || imageObjectKey.trim().isEmpty()) {
            return null;
        }

        return s3PresignedUrlService.createDownloadPresignedUrl(imageObjectKey);
    }
}
