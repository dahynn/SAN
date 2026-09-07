package com.san.api.global.external.ai.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.AiErrorCode;
import com.san.api.global.external.ai.dto.request.AiAnalyzeRequest;
import com.san.api.global.external.ai.dto.request.AiGithubStarRecommendationRequest;
import com.san.api.global.external.ai.dto.response.AiGithubStarRecommendationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AiGithubStarRecommendationClientTest {

    private AiGithubStarRecommendationClient client;
    private RestClient restClient;
    private RestClient.RequestBodyUriSpec postSpec;
    private RestClient.ResponseSpec responseSpec;

    @BeforeEach
    void setUp() {
        restClient = mock(RestClient.class);
        postSpec = mock(RestClient.RequestBodyUriSpec.class, RETURNS_SELF);
        responseSpec = mock(RestClient.ResponseSpec.class);

        when(restClient.post()).thenReturn(postSpec);
        when(postSpec.retrieve()).thenReturn(responseSpec);

        client = new AiGithubStarRecommendationClient(restClient, new ObjectMapper());
    }

    @Test
    void recommend_returnsAnalyzeCompatibleUrlInputs() {
        AiGithubStarRecommendationResponse response = new AiGithubStarRecommendationResponse(List.of(
                new AiAnalyzeRequest("url", "https://react.dev/learn/managing-state")
        ));
        when(responseSpec.body(AiGithubStarRecommendationResponse.class)).thenReturn(response);

        List<AiAnalyzeRequest> result = client.recommend(new AiGithubStarRecommendationRequest("octocat", 5));

        assertThat(result).containsExactly(new AiAnalyzeRequest("url", "https://react.dev/learn/managing-state"));
    }

    @Test
    void recommend_throwsWhenResponseHasInvalidInputType() {
        AiGithubStarRecommendationResponse response = new AiGithubStarRecommendationResponse(List.of(
                new AiAnalyzeRequest("text", "https://react.dev/learn/managing-state")
        ));
        when(responseSpec.body(AiGithubStarRecommendationResponse.class)).thenReturn(response);

        assertThatThrownBy(() -> client.recommend(new AiGithubStarRecommendationRequest("octocat", 5)))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(AiErrorCode.AI_GITHUB_STAR_RECOMMENDATION_INVALID_RESPONSE));
    }

    @Test
    void recommend_mapsGithubUserNotFoundError() {
        HttpClientErrorException exception = HttpClientErrorException.create(
                HttpStatus.NOT_FOUND,
                "Not Found",
                null,
                "{\"error_code\":\"github_user_not_found\"}".getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8
        );
        when(responseSpec.body(AiGithubStarRecommendationResponse.class)).thenThrow(exception);

        assertThatThrownBy(() -> client.recommend(new AiGithubStarRecommendationRequest("unknown", 5)))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(AiErrorCode.AI_GITHUB_USER_NOT_FOUND));
    }
}
