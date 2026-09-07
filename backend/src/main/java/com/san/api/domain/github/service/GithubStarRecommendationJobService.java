package com.san.api.domain.github.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.san.api.domain.github.dto.response.GithubStarRecommendationJobResponse;
import com.san.api.domain.github.dto.response.GithubStarRecommendationListResponse;
import com.san.api.domain.github.entity.GithubStarRecommendation;
import com.san.api.domain.github.repository.GithubStarRecommendationRepository;
import com.san.api.global.async.entity.AsyncJob;
import com.san.api.global.async.entity.JobStatus;
import com.san.api.global.async.entity.JobType;
import com.san.api.global.async.repository.AsyncJobRepository;
import com.san.api.global.async.service.AsyncJobManager;
import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.CommonErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** GitHub Star 추천 작업 Service */
@Service
@RequiredArgsConstructor
public class GithubStarRecommendationJobService {

    private final GithubStarRecommendationRepository githubStarRecommendationRepository;
    private final AsyncJobManager asyncJobManager;
    private final AsyncJobRepository asyncJobRepository;
    private final ObjectMapper objectMapper;

    /**
     * GitHub Star 추천 작업 요청
     *
     * @param userId 사용자 ID
     * @return 기존 추천 후보 또는 등록된 작업 ID
     */
    @Transactional
    public GithubStarRecommendationJobResponse requestRecommendation(UUID userId) {
        List<GithubStarRecommendation> recommendations = findUserRecommendations(userId);
        if (!recommendations.isEmpty()) {
            return GithubStarRecommendationJobResponse.alreadyRecommended(recommendations, objectMapper);
        }

        Optional<UUID> activeJobId = findActiveRecommendationJobId(userId);
        if (activeJobId.isPresent()) {
            return GithubStarRecommendationJobResponse.created(activeJobId.get());
        }

        UUID jobId = enqueueRecommendationJob(userId);
        return GithubStarRecommendationJobResponse.created(jobId);
    }

    /**
     * 사용자 GitHub Star 추천 후보 목록 조회
     *
     * @param userId 사용자 ID
     * @return GitHub Star 추천 후보 목록 응답
     */
    @Transactional(readOnly = true)
    public GithubStarRecommendationListResponse getRecommendations(UUID userId) {
        return GithubStarRecommendationListResponse.from(findUserRecommendations(userId), objectMapper);
    }

    private List<GithubStarRecommendation> findUserRecommendations(UUID userId) {
        return githubStarRecommendationRepository.findAllByUser_UserIdOrderByCreatedAtDesc(userId);
    }

    private UUID enqueueRecommendationJob(UUID userId) {
        try {
            return asyncJobManager.enqueue(JobType.GITHUB_STAR_RECOMMENDATION, userId);
        } catch (BusinessException e) {
            if (e.getErrorCode() != CommonErrorCode.DUPLICATE_RESOURCE) {
                throw e;
            }
            return findActiveRecommendationJobId(userId)
                    .orElseThrow(() -> e);
        }
    }

    private Optional<UUID> findActiveRecommendationJobId(UUID userId) {
        return asyncJobRepository.findByTargetIdAndJobType(userId, JobType.GITHUB_STAR_RECOMMENDATION)
                .stream()
                .filter(this::isActiveJob)
                .map(AsyncJob::getJobId)
                .findFirst();
    }

    private boolean isActiveJob(AsyncJob job) {
        return job.getStatus() == JobStatus.PENDING || job.getStatus() == JobStatus.PROCESSING;
    }
}
