package com.san.api.global.external.ai.client;

import com.san.api.global.async.config.RetryConfig;
import com.san.api.global.exception.BusinessException;
import com.san.api.global.external.ai.dto.request.AiQuizContentRequest;
import com.san.api.global.external.ai.dto.request.AiQuizRequest;
import com.san.api.global.external.ai.dto.response.AiOxQuizQuestionResponse;
import com.san.api.global.external.ai.dto.response.AiOxQuizResponse;
import com.san.api.global.external.ai.dto.response.AiShortAnswerQuizQuestionResponse;
import com.san.api.global.external.ai.dto.response.AiShortAnswerQuizResponse;
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

@SpringBootTest(classes = {AiQuizClientRetryTest.MockRestClientConfig.class, RetryConfig.class, AiQuizClient.class})
class AiQuizClientRetryTest {

    @TestConfiguration
    static class MockRestClientConfig {
        @Bean("aiRestClient")
        RestClient aiRestClient() {
            return mock(RestClient.class);
        }
    }

    @Autowired
    private AiQuizClient aiQuizClient;

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
    void generateShortAnswerQuizReturnsResponse() {
        AiShortAnswerQuizResponse response = new AiShortAnswerQuizResponse(
                "short_answer",
                List.of(new AiShortAnswerQuizQuestionResponse("질문", "정답", "해설"))
        );
        when(responseSpec.body(AiShortAnswerQuizResponse.class)).thenReturn(response);

        AiQuizRequest request = shortAnswerRequest();

        AiShortAnswerQuizResponse result = aiQuizClient.generateShortAnswerQuiz(request);

        assertThat(result.quizType()).isEqualTo("short_answer");
        assertThat(result.questions()).hasSize(1);
        assertThat(result.questions().getFirst().answer()).isEqualTo("정답");
        verify(restClient, times(1)).post();
    }

    @Test
    void generateOxQuizReturnsResponse() {
        AiOxQuizResponse response = new AiOxQuizResponse(
                "ox",
                List.of(new AiOxQuizQuestionResponse("판단 문장", true, "해설"))
        );
        when(responseSpec.body(AiOxQuizResponse.class)).thenReturn(response);

        AiQuizRequest request = oxRequest();

        AiOxQuizResponse result = aiQuizClient.generateOxQuiz(request);

        assertThat(result.quizType()).isEqualTo("ox");
        assertThat(result.questions()).hasSize(1);
        assertThat(result.questions().getFirst().isCorrect()).isTrue();
        verify(restClient, times(1)).post();
    }

    @Test
    void invalidResponseThrowsExceptionWithoutRetry() {
        AiShortAnswerQuizResponse response = new AiShortAnswerQuizResponse(
                "short_answer",
                List.of(new AiShortAnswerQuizQuestionResponse("", "정답", "해설"))
        );
        when(responseSpec.body(AiShortAnswerQuizResponse.class)).thenReturn(response);

        AiQuizRequest request = shortAnswerRequest();

        assertThatThrownBy(() -> aiQuizClient.generateShortAnswerQuiz(request))
                .isInstanceOf(BusinessException.class);

        verify(restClient, times(1)).post();
    }

    @Test
    void restClientExceptionRetriesThreeTimes() {
        when(responseSpec.body(AiOxQuizResponse.class))
                .thenThrow(new RestClientException("connection timeout"));

        AiQuizRequest request = oxRequest();

        assertThatThrownBy(() -> aiQuizClient.generateOxQuiz(request))
                .isInstanceOf(BusinessException.class);

        verify(restClient, times(3)).post();
    }

    @Test
    void temporaryFailureThenSuccessReturnsResponse() {
        AiOxQuizResponse response = new AiOxQuizResponse(
                "ox",
                List.of(new AiOxQuizQuestionResponse("판단 문장", false, "해설"))
        );
        when(responseSpec.body(AiOxQuizResponse.class))
                .thenThrow(new RestClientException("temporary error"))
                .thenReturn(response);

        AiQuizRequest request = oxRequest();

        AiOxQuizResponse result = aiQuizClient.generateOxQuiz(request);

        assertThat(result.questions().getFirst().isCorrect()).isFalse();
        verify(restClient, times(2)).post();
    }

    private AiQuizRequest shortAnswerRequest() {
        return new AiQuizRequest(
                List.of(new AiQuizContentRequest("text", "Docker는 컨테이너 플랫폼이다.")),
                "short_answer"
        );
    }

    private AiQuizRequest oxRequest() {
        return new AiQuizRequest(
                List.of(new AiQuizContentRequest("text", "React.memo는 메모이제이션 도구다.")),
                "ox"
        );
    }
}
