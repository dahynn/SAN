package com.san.api.domain.scrap.service;

import com.san.api.domain.scrap.dto.request.ScrapCreateRequest;
import com.san.api.domain.scrap.dto.response.ScrapResponse;
import com.san.api.domain.knowledge.entity.KnowledgeCard;
import com.san.api.domain.knowledge.repository.KnowledgeCardRepository;
import com.san.api.domain.scrap.entity.Scrap;
import com.san.api.domain.scrap.entity.ScrapCardCreationStatus;
import com.san.api.domain.scrap.entity.ScrapOriginStatus;
import com.san.api.domain.scrap.entity.ScrapRefineStatus;
import com.san.api.domain.scrap.entity.SourceType;
import com.san.api.domain.scrap.repository.ScrapRepository;
import com.san.api.domain.user.entity.User;
import com.san.api.domain.user.repository.UserRepository;
import com.san.api.global.async.entity.AsyncJob;
import com.san.api.global.async.entity.JobStatus;
import com.san.api.global.async.entity.JobType;
import com.san.api.global.async.repository.AsyncJobRepository;
import com.san.api.global.async.service.AsyncJobManager;
import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.CommonErrorCode;
import com.san.api.global.exception.errorcode.ScrapErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/** 수집 원본 저장 및 지식카드 분석 작업 등록 Service */
@Service
@RequiredArgsConstructor
public class ScrapService {

    private final ScrapRepository scrapRepository;
    private final UserRepository userRepository;
    private final SourceTypeDetector sourceTypeDetector;
    private final ScrapContentHashPolicy contentHashPolicy;
    private final AsyncJobManager asyncJobManager;
    private final AsyncJobRepository asyncJobRepository;
    private final KnowledgeCardRepository knowledgeCardRepository;

    /**
     * 수집 원본 저장 후 지식카드 분석 작업 등록
     *
     * @param userId 로그인 사용자 ID
     * @param request 수집 원본 저장 요청
     * @return 저장된 수집 원본과 분석 작업 또는 지식카드 ID 응답
     */
    public ScrapResponse createScrap(UUID userId, ScrapCreateRequest request) {
        validateRawContent(request.rawContent());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND));
        String normalizedRawContent = contentHashPolicy.normalize(request.rawContent());
        String contentHash = contentHashPolicy.createContentHash(normalizedRawContent);
        String imageObjectKey = blankToNull(request.imageObjectKey());
        SourceType sourceType = detectSourceType(normalizedRawContent, imageObjectKey);

        Optional<Scrap> existingScrap = scrapRepository.findByUser_UserIdAndSourceTypeAndContentHash(
                userId,
                sourceType,
                contentHash
        );
        if (existingScrap.isPresent()) {
            Scrap scrap = existingScrap.get();
            UUID refineJobId = enqueueScrapRefineJobIfNeeded(scrap);
            return createResponseWithJob(scrap, refineJobId, true);
        }

        Scrap scrap = Scrap.builder()
                .user(user)
                .sourceType(sourceType)
                .sourceUrl(blankToNull(request.sourceUrl()))
                .rawContent(normalizedRawContent)
                .contentHash(contentHash)
                .imageObjectKey(imageObjectKey)
                .build();

        SaveScrapResult saveResult = saveScrap(scrap, userId, sourceType, contentHash);
        Scrap savedScrap = saveResult.scrap();
        UUID refineJobId = enqueueScrapRefineJob(savedScrap.getScrapId());

        return createResponseWithJob(savedScrap, refineJobId, saveResult.existing());
    }

    /** 스크랩 저장 중 유니크 충돌이 발생하면 기존 스크랩을 재조회 */
    private SaveScrapResult saveScrap(Scrap scrap, UUID userId, SourceType sourceType, String contentHash) {
        try {
            return new SaveScrapResult(scrapRepository.save(scrap), false);
        } catch (DataIntegrityViolationException e) {
            return new SaveScrapResult(findExistingScrap(userId, sourceType, contentHash), true);
        }
    }

    /** 스크랩에 연결된 활성 분석 작업을 조회하거나 새로 등록 */
    private ScrapResponse createResponseWithJob(Scrap scrap, UUID refineJobId, boolean existingScrap) {
        Optional<KnowledgeCard> card = knowledgeCardRepository.findByScrapIdWithCategory(scrap.getScrapId());
        if (card.isPresent()) {
            return ScrapResponse.from(
                    scrap,
                    null,
                    refineJobId,
                    card.get().getCardId(),
                    resolveOriginStatus(existingScrap),
                    resolveRefineStatus(refineJobId),
                    ScrapCardCreationStatus.CARD_READY
            );
        }

        UUID analysisJobId = findActiveCardAnalysisJobId(scrap.getScrapId())
                .orElseGet(() -> enqueueCardAnalysisJob(scrap.getScrapId()));

        return ScrapResponse.from(
                scrap,
                analysisJobId,
                refineJobId,
                null,
                resolveOriginStatus(existingScrap),
                resolveRefineStatus(refineJobId),
                ScrapCardCreationStatus.ANALYSIS_IN_PROGRESS
        );
    }

    private ScrapOriginStatus resolveOriginStatus(boolean existingScrap) {
        return existingScrap ? ScrapOriginStatus.EXISTING : ScrapOriginStatus.CREATED;
    }

    private ScrapRefineStatus resolveRefineStatus(UUID refineJobId) {
        return refineJobId == null ? ScrapRefineStatus.REFINE_COMPLETED : ScrapRefineStatus.REFINE_IN_PROGRESS;
    }

    private SourceType detectSourceType(String normalizedRawContent, String imageObjectKey) {
        if (!isBlank(imageObjectKey)) {
            return SourceType.IMAGE;
        }

        return sourceTypeDetector.detect(normalizedRawContent);
    }

    /** 지식카드 분석 작업 등록 중 중복 충돌이 발생하면 활성 작업을 재조회 */
    private UUID enqueueCardAnalysisJob(UUID scrapId) {
        try {
            return asyncJobManager.enqueue(JobType.CARD_ANALYSIS, scrapId);
        } catch (BusinessException e) {
            if (e.getErrorCode() != CommonErrorCode.DUPLICATE_RESOURCE) {
                throw e;
            }
            return findActiveCardAnalysisJobId(scrapId)
                    .orElseThrow(() -> e);
        }
    }

    /** 원본 정제 작업 등록 */
    private UUID enqueueScrapRefineJob(UUID scrapId) {
        try {
            return asyncJobManager.enqueue(JobType.SCRAP_REFINE, scrapId);
        } catch (BusinessException e) {
            if (e.getErrorCode() != CommonErrorCode.DUPLICATE_RESOURCE) {
                throw e;
            }
            return findActiveScrapRefineJobId(scrapId)
                    .orElseThrow(() -> e);
        }
    }

    /** 원본 정제 내용이 없으면 정제 작업 등록 */
    private UUID enqueueScrapRefineJobIfNeeded(Scrap scrap) {
        if (isBlank(scrap.getRefinedContent())) {
            return enqueueScrapRefineJob(scrap.getScrapId());
        }
        return null;
    }

    /** 진행 중인 지식카드 분석 작업 ID 조회 */
    private Optional<UUID> findActiveCardAnalysisJobId(UUID scrapId) {
        return asyncJobRepository.findByTargetIdAndJobType(scrapId, JobType.CARD_ANALYSIS)
                .stream()
                .filter(this::isActiveJob)
                .map(AsyncJob::getJobId)
                .findFirst();
    }

    /** 진행 중인 원본 정제 작업 ID 조회 */
    private Optional<UUID> findActiveScrapRefineJobId(UUID scrapId) {
        return asyncJobRepository.findByTargetIdAndJobType(scrapId, JobType.SCRAP_REFINE)
                .stream()
                .filter(this::isActiveJob)
                .map(AsyncJob::getJobId)
                .findFirst();
    }

    /** 활성 작업 여부 */
    private boolean isActiveJob(AsyncJob job) {
        return job.getStatus() == JobStatus.PENDING || job.getStatus() == JobStatus.PROCESSING;
    }

    /** 기존 스크랩 조회 */
    private Scrap findExistingScrap(UUID userId, SourceType sourceType, String contentHash) {
        return scrapRepository.findByUser_UserIdAndSourceTypeAndContentHash(userId, sourceType, contentHash)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.DUPLICATE_RESOURCE));
    }

    /**
     * 원본 입력값 검증
     *
     * @param rawContent 사용자가 입력하거나 붙여넣은 원본 값
     */
    private void validateRawContent(String rawContent) {
        if (isBlank(rawContent)) {
            throw new BusinessException(ScrapErrorCode.EMPTY_SOURCE);
        }
    }

    /**
     * 빈 문자열 null 변환
     *
     * @param value 변환 대상 값
     * @return 공백 제거 값 또는 null
     */
    private String blankToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    /**
     * 빈 값 여부
     *
     * @param value 검사할 값
     * @return null, 빈 문자열, 공백 문자열 여부
     */
    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private record SaveScrapResult(Scrap scrap, boolean existing) {
    }
}
