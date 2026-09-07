package com.san.api.global.external.ai.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.AiErrorCode;
import com.san.api.global.exception.errorcode.ErrorCode;
import com.san.api.global.external.ai.dto.request.AiAnalyzeRequest;
import com.san.api.global.external.ai.dto.request.AiGithubStarRecommendationRequest;
import com.san.api.global.external.ai.dto.response.AiErrorResponse;
import com.san.api.global.external.ai.dto.response.AiGithubStarRecommendationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

/** AI GitHub Star 추천 Client */
@Slf4j
@Component
public class AiGithubStarRecommendationClient {

    private static final String URL_INPUT_TYPE = "url";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public AiGithubStarRecommendationClient(
            @Qualifier("aiRestClient") RestClient restClient,
            ObjectMapper objectMapper
    ) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
    }

    /** AI 서버에 GitHub Star 추천 URL 목록을 요청 */
    @Retryable(
            retryFor = {RestClientException.class},
            noRetryFor = {BusinessException.class, HttpClientErrorException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2.0)
    )
    public List<AiAnalyzeRequest> recommend(AiGithubStarRecommendationRequest request) {
        try {
            AiGithubStarRecommendationResponse response = restClient.post()
                    .uri("/ai/recommend/github-stars")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(AiGithubStarRecommendationResponse.class);

            validateResponse(response);
            return response.recommendations();
        } catch (HttpClientErrorException e) {
            throw new BusinessException(toErrorCode(e));
        }
    }

    @Recover
    public List<AiAnalyzeRequest> recoverRecommend(Exception e, AiGithubStarRecommendationRequest request) {
        if (e instanceof BusinessException be) {
            throw be;
        }
        log.error("AI GitHub Star recommendation failed after all retries: {}", e.getMessage(), e);
        throw new BusinessException(AiErrorCode.AI_GITHUB_STAR_RECOMMENDATION_FAILED);
    }

    private void validateResponse(AiGithubStarRecommendationResponse response) {
        if (response == null
                || response.recommendations() == null
                || response.recommendations().isEmpty()
                || response.recommendations().stream().anyMatch(this::isInvalidRecommendation)) {
            throw new BusinessException(AiErrorCode.AI_GITHUB_STAR_RECOMMENDATION_INVALID_RESPONSE);
        }
    }

    private boolean isInvalidRecommendation(AiAnalyzeRequest recommendation) {
        return recommendation == null
                || !URL_INPUT_TYPE.equals(recommendation.inputType())
                || isBlank(recommendation.content());
    }

    private ErrorCode toErrorCode(HttpClientErrorException e) {
        String aiErrorCode = parseAiErrorCode(e.getResponseBodyAsString());
        return switch (aiErrorCode) {
            case "missing_github_username", "invalid_limit" ->
                    AiErrorCode.AI_GITHUB_STAR_RECOMMENDATION_INVALID_REQUEST;
            case "github_user_not_found" -> AiErrorCode.AI_GITHUB_USER_NOT_FOUND;
            case "github_fetch_failed" -> AiErrorCode.AI_GITHUB_STAR_FETCH_FAILED;
            case "search_failed" -> AiErrorCode.AI_GITHUB_STAR_SEARCH_FAILED;
            case "recommendation_failed" -> AiErrorCode.AI_GITHUB_STAR_RECOMMENDATION_FAILED;
            default -> AiErrorCode.AI_GITHUB_STAR_RECOMMENDATION_FAILED;
        };
    }

    private String parseAiErrorCode(String responseBody) {
        if (isBlank(responseBody)) {
            return "";
        }
        try {
            AiErrorResponse response = objectMapper.readValue(responseBody, AiErrorResponse.class);
            return response.errorCode() == null ? "" : response.errorCode();
        } catch (JsonProcessingException e) {
            return "";
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
