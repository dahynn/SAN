package com.san.api.domain.github.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.san.api.domain.github.entity.GithubStarRecommendation;
import com.san.api.domain.github.repository.GithubStarRecommendationRepository;
import com.san.api.domain.user.entity.User;
import com.san.api.domain.user.repository.UserRepository;
import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.AiErrorCode;
import com.san.api.global.exception.errorcode.CommonErrorCode;
import com.san.api.global.external.ai.client.AiAnalysisClient;
import com.san.api.global.external.ai.dto.request.AiAnalyzeRequest;
import com.san.api.global.external.ai.dto.response.AiAnalyzeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** GitHub Star 추천 URL 분석 저장 Service */
@Service
@RequiredArgsConstructor
public class GithubStarRecommendationAnalysisService {

    private final UserRepository userRepository;
    private final GithubStarRecommendationService githubStarRecommendationService;
    private final GithubStarRecommendationRepository githubStarRecommendationRepository;
    private final AiAnalysisClient aiAnalysisClient;
    private final ObjectMapper objectMapper;

    /**
     * GitHub Star 추천 URL을 분석해 추천 후보로 저장
     *
     * 추천 URL 목록은 AI 추천 API에서 받고, 각 URL의 제목/요약/분석 결과는 기존 AI 분석 API 사용
     */
    @Transactional
    public List<GithubStarRecommendation> analyzeAndSave(UUID userId, UUID jobId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND));
        List<AiAnalyzeRequest> recommendations = githubStarRecommendationService.recommendAnalyzeInputs(userId);

        List<GithubStarRecommendation> savedRecommendations = recommendations.stream()
                .map(recommendation -> analyzeAndCreateRecommendation(user, jobId, recommendation))
                .toList();

        return githubStarRecommendationRepository.saveAll(savedRecommendations);
    }

    /** 추천 URL 1개를 기존 AI 분석 결과 기반 추천 후보로 변환 */
    private GithubStarRecommendation analyzeAndCreateRecommendation(
            User user,
            UUID jobId,
            AiAnalyzeRequest recommendation
    ) {
        AiAnalyzeResponse analysis = aiAnalysisClient.analyze(recommendation);
        return new GithubStarRecommendation(
                user,
                jobId,
                recommendation.content().trim(),
                analysis.title().trim(),
                analysis.summary().trim(),
                toAnalysisResult(analysis)
        );
    }

    /** 수집 확정 시 재사용할 AI 분석 결과 JSON 변환 */
    private String toAnalysisResult(AiAnalyzeResponse analysis) {
        try {
            return objectMapper.writeValueAsString(analysis);
        } catch (JsonProcessingException e) {
            throw new BusinessException(AiErrorCode.AI_ANALYSIS_INVALID_RESPONSE);
        }
    }
}
