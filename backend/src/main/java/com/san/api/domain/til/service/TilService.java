package com.san.api.domain.til.service;

import com.san.api.domain.knowledge.dto.response.KnowledgeCardResponse;
import com.san.api.domain.knowledge.dto.response.CategoryResponse;
import com.san.api.domain.knowledge.entity.CardTag;
import com.san.api.domain.knowledge.entity.KnowledgeCard;
import com.san.api.domain.knowledge.repository.CardTagRepository;
import com.san.api.domain.knowledge.repository.KnowledgeCardRepository;
import com.san.api.domain.knowledge.service.VectorSearchService;
import com.san.api.domain.scrap.entity.Scrap;
import com.san.api.domain.til.dto.request.TilGenerateRequest;
import com.san.api.domain.til.dto.request.TilUpdateRequest;
import com.san.api.domain.til.dto.response.TilGenerationJobResponse;
import com.san.api.domain.til.dto.response.TilRecallCardsResponse;
import com.san.api.domain.til.dto.response.TilResponse;
import com.san.api.domain.til.dto.response.TilSourceContentResponse;
import com.san.api.domain.til.dto.response.TilSourcesResponse;
import com.san.api.domain.til.entity.DailySummary;
import com.san.api.domain.til.repository.DailySummaryRepository;
import com.san.api.global.async.entity.JobStatus;
import com.san.api.global.async.entity.JobType;
import com.san.api.global.async.service.AsyncJobManager;
import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.CommonErrorCode;
import com.san.api.global.exception.errorcode.TilErrorCode;
import com.san.api.global.external.ai.client.AiEmbeddingClient;
import com.san.api.global.external.s3.service.S3PresignedUrlService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/** TIL 생성 작업 등록 및 조회 Service */
@Service
@RequiredArgsConstructor
public class TilService {

    private final DailySummaryRepository dailySummaryRepository;
    private final DailySummaryService dailySummaryService;
    private final AsyncJobManager asyncJobManager;
    private final VectorSearchService vectorSearchService;
    private final CardTagRepository cardTagRepository;
    private final KnowledgeCardRepository knowledgeCardRepository;
    private final S3PresignedUrlService s3PresignedUrlService;
    private final AiEmbeddingClient aiEmbeddingClient;

    /**
     * TIL 생성 비동기 작업 등록
     *
     * @param userId  로그인 사용자 ID
     * @param request TIL 생성 작업 등록 요청
     * @return 등록된 TIL 생성 작업 응답
     */
    @Transactional
    public TilGenerationJobResponse requestGeneration(UUID userId, TilGenerateRequest request) {
        dailySummaryRepository.acquireGenerationLock(userId);
        validateNoActiveGenerationJob(userId);

        DailySummary summary = dailySummaryService.createSummary(userId, request.targetDate());
        UUID jobId = asyncJobManager.enqueueInCurrentTransaction(JobType.TIL_GENERATION, summary.getSummaryId());

        return new TilGenerationJobResponse(
                summary.getSummaryId(),
                jobId,
                summary.getTargetDate()
        );
    }

    private void validateNoActiveGenerationJob(UUID userId) {
        if (dailySummaryRepository.existsActiveJobForUser(
                userId,
                JobType.TIL_GENERATION,
                List.of(JobStatus.PENDING, JobStatus.PROCESSING)
        )) {
            throw new BusinessException(CommonErrorCode.DUPLICATE_RESOURCE, "TIL generation job is already in progress.");
        }
    }

    /**
     * TIL 삭제
     *
     * @param summaryId TIL ID
     * @param userId    로그인 사용자 ID
     */
    @Transactional
    public void deleteTil(UUID summaryId, UUID userId) {
        DailySummary summary = dailySummaryRepository.findBySummaryIdWithUser(summaryId)
                .orElseThrow(() -> new BusinessException(TilErrorCode.SUMMARY_NOT_FOUND));
        validateSummaryOwner(summary, userId);

        summary.deleteSummary();
    }

    /**
     * TIL 제목과 내용을 수정
     *
     * @param summaryId TIL ID
     * @param userId    로그인 사용자 ID
     * @param request   TIL 수정 요청
     * @return 수정된 TIL 응답
     */
    @Transactional
    public TilResponse updateTil(UUID summaryId, UUID userId, TilUpdateRequest request) {
        DailySummary summary = dailySummaryRepository.findBySummaryIdWithUser(summaryId)
                .orElseThrow(() -> new BusinessException(TilErrorCode.SUMMARY_NOT_FOUND));
        validateSummaryOwner(summary, userId);

        float[] embedding = aiEmbeddingClient.embed(request.content());
        summary.update(request.title(), request.content(), embedding);

        return TilResponse.from(summary);
    }

    /**
     * 날짜 기준 TIL 조회
     *
     * @param userId     로그인 사용자 ID
     * @param targetDate 조회 대상 날짜
     * @return 날짜 기준 TIL 조회 응답
     */
    @Transactional(readOnly = true)
    public List<TilResponse> getTil(UUID userId, LocalDate targetDate) {
        return dailySummaryRepository.findAllByUser_UserIdAndTargetDateOrderByCreatedAtDesc(userId, targetDate)
                .stream()
                .map(TilResponse::from)
                .toList();
    }

    /**
     * TIL 생성에 사용된 원본 지식카드 목록 조회
     *
     * @param summaryId TIL ID
     * @param userId 로그인 사용자 ID
     * @return TIL 생성 원본 목록 응답
     */
    @Transactional(readOnly = true)
    public TilSourcesResponse getSources(UUID summaryId, UUID userId) {
        DailySummary summary = dailySummaryRepository.findBySummaryIdWithUser(summaryId)
                .orElseThrow(() -> new BusinessException(TilErrorCode.SUMMARY_NOT_FOUND));
        validateSummaryOwner(summary, userId);

        List<TilSourceContentResponse> sources = knowledgeCardRepository.findTilSourceCards(
                        userId,
                        summary.getTargetDate().atStartOfDay(),
                        summary.getTargetDate().plusDays(1).atStartOfDay()
                )
                .stream()
                .map(this::toSourceItem)
                .toList();

        return new TilSourcesResponse(sources);
    }

    /**
     * TIL 기반 리콜 카드 조회.
     * TIL 임베딩과 유사한 카드를 threshold 기준으로 전체 반환한다.
     *
     * @param summaryId TIL ID
     * @param userId    로그인 사용자 ID
     * @return 리콜 카드 목록
     */
    @Transactional(readOnly = true)
    public TilRecallCardsResponse getRecallCards(UUID summaryId, UUID userId) {
        List<KnowledgeCard> cards = vectorSearchService.findRelatedByTil(summaryId, userId);

        if (cards.isEmpty()) {
            return new TilRecallCardsResponse(List.of());
        }

        Map<UUID, List<CardTag>> tagsByCardId = cardTagRepository.findAllByKnowledgeCardInWithTag(cards)
                .stream()
                .collect(Collectors.groupingBy(ct -> ct.getKnowledgeCard().getCardId()));

        List<KnowledgeCardResponse> responses = cards.stream()
                .map(card -> KnowledgeCardResponse.from(
                        card,
                        tagsByCardId.getOrDefault(card.getCardId(), List.of())
                ))
                .toList();

        return new TilRecallCardsResponse(responses);
    }

    /**
     * 지식카드를 TIL 생성 원본 표시용 응답으로 변환
     *
     * @param card TIL 생성에 사용된 지식카드
     * @return TIL 생성 원본 단건 응답
     */
    private TilSourceContentResponse toSourceItem(KnowledgeCard card) {
        Scrap scrap = card.getScrap();
        return new TilSourceContentResponse(
                card.getCardId(),
                scrap.getScrapId(),
                card.getTitle(),
                scrap.getSourceType(),
                scrap.getRawContent(),
                scrap.getSourceUrl(),
                createImageUrl(scrap.getImageObjectKey()),
                new CategoryResponse(
                        card.getCategory().getCategoryId(),
                        card.getCategory().getCategoryName()
                ),
                card.getCreatedAt()
        );
    }

    private String createImageUrl(String imageObjectKey) {
        if (imageObjectKey == null || imageObjectKey.trim().isEmpty()) {
            return null;
        }

        return s3PresignedUrlService.createDownloadPresignedUrl(imageObjectKey);
    }

    /**
     * TIL 소유자 검증
     *
     * @param summary 조회된 TIL
     * @param userId 로그인 사용자 ID
     */
    private void validateSummaryOwner(DailySummary summary, UUID userId) {
        if (!summary.getUser().getUserId().equals(userId)) {
            throw new BusinessException(TilErrorCode.SUMMARY_ACCESS_DENIED);
        }
    }
}
