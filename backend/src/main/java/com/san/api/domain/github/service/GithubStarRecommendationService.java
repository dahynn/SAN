package com.san.api.domain.github.service;

import com.san.api.domain.github.entity.GithubAccount;
import com.san.api.domain.github.repository.GithubAccountRepository;
import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.AiErrorCode;
import com.san.api.global.exception.errorcode.AuthErrorCode;
import com.san.api.global.external.ai.client.AiGithubStarRecommendationClient;
import com.san.api.global.external.ai.dto.request.AiAnalyzeRequest;
import com.san.api.global.external.ai.dto.request.AiGithubStarRecommendationRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** GitHub Star 추천 Service */
@Service
@RequiredArgsConstructor
public class GithubStarRecommendationService {

    private static final int MIN_RECOMMENDATION_LIMIT = 1;
    private static final int MAX_RECOMMENDATION_LIMIT = 10;

    private final GithubAccountRepository githubAccountRepository;
    private final AiGithubStarRecommendationClient aiGithubStarRecommendationClient;

    @Value("${ai.recommendation.github-stars.limit:5}")
    private int recommendationLimit;

    /** 연동된 GitHub 계정 기준으로 추천 URL 목록 요청 */
    @Transactional(readOnly = true)
    public List<AiAnalyzeRequest> recommendAnalyzeInputs(UUID userId) {
        validateRecommendationLimit();
        GithubAccount githubAccount = githubAccountRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new BusinessException(AuthErrorCode.GITHUB_ACCOUNT_NOT_LINKED));

        return aiGithubStarRecommendationClient.recommend(new AiGithubStarRecommendationRequest(
                githubAccount.getGithubUsername(),
                recommendationLimit
        ));
    }

    private void validateRecommendationLimit() {
        if (recommendationLimit < MIN_RECOMMENDATION_LIMIT || recommendationLimit > MAX_RECOMMENDATION_LIMIT) {
            throw new BusinessException(AiErrorCode.AI_GITHUB_STAR_RECOMMENDATION_INVALID_REQUEST);
        }
    }
}
