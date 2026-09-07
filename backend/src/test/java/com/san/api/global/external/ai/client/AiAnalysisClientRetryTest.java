package com.san.api.global.external.ai.client;

import com.san.api.global.async.config.RetryConfig;
import com.san.api.global.exception.BusinessException;
import com.san.api.global.external.ai.dto.request.AiAnalyzeRequest;
import com.san.api.global.external.ai.dto.response.AiAnalyzeResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * @Retryable 동작 검증 — Spring AOP 프록시가 필요하므로 Spring 컨텍스트를 사용한다.
 *
 * 검증 항목:
 *   - RestClientException(5xx/timeout) → 최대 3회 시도 후 예외 전파
 *   - BusinessException(응답 검증 실패) → 재시도 없이 즉시 전파
 *   - 1회 실패 후 성공 → 최종 결과 반환
 */
@SpringBootTest(classes = {AiAnalysisClientRetryTest.MockRestClientConfig.class, RetryConfig.class, AiAnalysisClient.class})
class AiAnalysisClientRetryTest {

    @TestConfiguration
    static class MockRestClientConfig {
        @Bean("aiRestClient")
        RestClient aiRestClient() {
            return mock(RestClient.class);
        }
    }

    @Autowired
    private AiAnalysisClient aiAnalysisClient;

    @Autowired
    @Qualifier("aiRestClient")
    private RestClient restClient;

    private RestClient.RequestBodyUriSpec postSpec;
    private RestClient.ResponseSpec responseSpec;

    @BeforeEach
    void setUp() {
        reset(restClient);
        postSpec = mock(RestClient.RequestBodyUriSpec.class, RETURNS_SELF);
        responseSpec = mock(RestClient.ResponseSpec.class);

        when(restClient.post()).thenReturn(postSpec);
        when(postSpec.retrieve()).thenReturn(responseSpec);
    }

    @Test
    void RestClientException_발생_시_3회_시도_후_예외를_전파한다() {
        when(responseSpec.body(AiAnalyzeResponse.class))
                .thenThrow(new RestClientException("connection timeout"));

        AiAnalyzeRequest request = new AiAnalyzeRequest("url", "https://example.com");

        assertThatThrownBy(() -> aiAnalysisClient.analyze(request))
                .isInstanceOf(BusinessException.class);

        verify(restClient, times(3)).post();
    }

    @Test
    void 첫_시도_실패_후_성공하면_결과를_반환한다() {
        AiAnalyzeResponse validResponse = new AiAnalyzeResponse(
                "제목", "요약", List.of("tag"), "카테고리", new float[]{0.1f}
        );
        when(responseSpec.body(AiAnalyzeResponse.class))
                .thenThrow(new RestClientException("일시적 오류"))
                .thenReturn(validResponse);

        AiAnalyzeRequest request = new AiAnalyzeRequest("url", "https://example.com");

        AiAnalyzeResponse result = aiAnalysisClient.analyze(request);

        assertThat(result.title()).isEqualTo("제목");
        verify(restClient, times(2)).post();
    }

    @Test
    void 응답_검증_실패_시_BusinessException을_재시도_없이_전파한다() {
        AiAnalyzeResponse invalidResponse = new AiAnalyzeResponse(
                null, null, null, null, null
        );
        when(responseSpec.body(AiAnalyzeResponse.class)).thenReturn(invalidResponse);

        AiAnalyzeRequest request = new AiAnalyzeRequest("url", "https://example.com");

        assertThatThrownBy(() -> aiAnalysisClient.analyze(request))
                .isInstanceOf(BusinessException.class);

        verify(restClient, times(1)).post();
    }
}
