package com.san.api.global.external.github.client;

import com.san.api.global.async.config.RetryConfig;
import com.san.api.global.exception.BusinessException;
import com.san.api.global.external.github.dto.response.GithubCreateContentResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * @Retryable 동작 검증 — Spring AOP 프록시가 필요하므로 Spring 컨텍스트를 사용한다.
 *
 * 검증 항목:
 *   - RestClientException(5xx/timeout) → 최대 3회 시도 후 예외 전파
 *   - HttpClientErrorException(4xx/409) → 재시도 없이 즉시 전파
 */
@SpringBootTest(classes = {RetryConfig.class, GithubApiClient.class})
@TestPropertySource(properties = {
        "oauth.github.client-id=test-id",
        "oauth.github.client-secret=test-secret",
        "oauth.github.redirect-uri=http://localhost/callback",
        "oauth.github.scope=repo"
})
class GithubApiClientRetryTest {

    @Autowired
    private GithubApiClient githubApiClient;

    private RestClient restClient;
    private RestClient.RequestBodyUriSpec putSpec;
    private RestClient.ResponseSpec responseSpec;

    @BeforeEach
    void setUp() {
        restClient = mock(RestClient.class);
        putSpec = mock(RestClient.RequestBodyUriSpec.class, RETURNS_SELF);
        responseSpec = mock(RestClient.ResponseSpec.class);

        ReflectionTestUtils.setField(githubApiClient, "restClient", restClient);

        when(restClient.put()).thenReturn(putSpec);
        when(putSpec.retrieve()).thenReturn(responseSpec);
    }

    @Test
    void RestClientException_발생_시_3회_시도_후_예외를_전파한다() {
        when(responseSpec.body(GithubCreateContentResponse.class))
                .thenThrow(new RestClientException("connection timeout"));

        assertThatThrownBy(() -> githubApiClient.createContent(
                "token", "owner", "repo", "path/file.md", "main", "commit msg", "base64content"
        )).isInstanceOf(BusinessException.class);

        verify(restClient, times(3)).put();
    }

    @Test
    void HttpClientErrorException_발생_시_재시도_없이_즉시_전파한다() {
        when(responseSpec.body(GithubCreateContentResponse.class))
                .thenThrow(HttpClientErrorException.create(
                        HttpStatus.CONFLICT, "Conflict", HttpHeaders.EMPTY, new byte[0], null
                ));

        assertThatThrownBy(() -> githubApiClient.createContent(
                "token", "owner", "repo", "path/file.md", "main", "commit msg", "base64content"
        )).isInstanceOf(BusinessException.class);

        verify(restClient, times(1)).put();
    }
}