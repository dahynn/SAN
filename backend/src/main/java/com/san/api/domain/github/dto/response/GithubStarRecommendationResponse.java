package com.san.api.domain.github.dto.response;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.san.api.domain.github.entity.GithubStarRecommendation;
import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.AiErrorCode;
import com.san.api.global.external.ai.dto.response.AiAnalyzeResponse;

import java.util.List;
import java.util.UUID;

/** GitHub Star 추천 후보 응답 DTO */
public record GithubStarRecommendationResponse(
        UUID recommendationId,
        String title,
        List<String> tagList,
        String recommendationUrl,
        boolean collected
) {

    public static GithubStarRecommendationResponse from(
            GithubStarRecommendation recommendation,
            ObjectMapper objectMapper
    ) {
        AiAnalyzeResponse analysis = parseAnalysisResult(recommendation.getAnalysisResult(), objectMapper);
        return new GithubStarRecommendationResponse(
                recommendation.getGithubStarRecommendationId(),
                recommendation.getTitle(),
                analysis.tags() == null ? List.of() : analysis.tags(),
                recommendation.getUrl(),
                recommendation.isCollected()
        );
    }

    private static AiAnalyzeResponse parseAnalysisResult(String analysisResult, ObjectMapper objectMapper) {
        try {
            return objectMapper.readValue(analysisResult, AiAnalyzeResponse.class);
        } catch (JsonProcessingException e) {
            throw new BusinessException(AiErrorCode.AI_ANALYSIS_INVALID_RESPONSE);
        }
    }
}
