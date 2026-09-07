package com.san.api.global.external.ai.client;

import com.san.api.global.async.config.RetryConfig;
import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.AiErrorCode;
import com.san.api.global.external.ai.dto.request.AiTilContentRequest;
import com.san.api.global.external.ai.dto.request.AiTilRequest;
import com.san.api.global.external.ai.dto.response.AiTilResponse;
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
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = {AiTilClientRetryTest.MockRestClientConfig.class, RetryConfig.class, AiTilClient.class})
class AiTilClientRetryTest {

    @TestConfiguration
    static class MockRestClientConfig {
        @Bean("aiRestClient")
        RestClient aiRestClient() {
            return mock(RestClient.class);
        }
    }

    @Autowired
    private AiTilClient aiTilClient;

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
    void generateTilReturnsResponse() {
        AiTilResponse response = new AiTilResponse("title", "# TIL", new float[]{0.1f});
        when(responseSpec.body(AiTilResponse.class)).thenReturn(response);

        AiTilResponse result = aiTilClient.generateTil(request());

        assertThat(result.title()).isEqualTo("title");
        verify(restClient, times(1)).post();
    }

    @Test
    void restClientExceptionDoesNotRetry() {
        when(responseSpec.body(AiTilResponse.class))
                .thenThrow(new RestClientException("read timeout"));

        assertThatThrownBy(() -> aiTilClient.generateTil(request()))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(AiErrorCode.AI_TIL_GENERATION_FAILED)
                );

        verify(restClient, times(1)).post();
    }

    @Test
    void invalidResponseThrowsExceptionWithoutRetry() {
        when(responseSpec.body(AiTilResponse.class))
                .thenReturn(new AiTilResponse("title", "", new float[]{0.1f}));

        assertThatThrownBy(() -> aiTilClient.generateTil(request()))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(AiErrorCode.AI_TIL_INVALID_RESPONSE)
                );

        verify(restClient, times(1)).post();
    }

    private AiTilRequest request() {
        return new AiTilRequest(
                List.of(new AiTilContentRequest("text", "Spring transaction summary")),
                true
        );
    }
}
