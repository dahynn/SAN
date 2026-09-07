package com.san.api.global.external.ai.client;

import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.AiErrorCode;
import com.san.api.global.external.ai.dto.request.AiQuizRequest;
import com.san.api.global.external.ai.dto.response.AiOxQuizQuestionResponse;
import com.san.api.global.external.ai.dto.response.AiOxQuizResponse;
import com.san.api.global.external.ai.dto.response.AiShortAnswerQuizQuestionResponse;
import com.san.api.global.external.ai.dto.response.AiShortAnswerQuizResponse;
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

/** AI 리콜 퀴즈 생성 Client */
@Slf4j
@Component
public class AiQuizClient {

    private static final String SHORT_ANSWER_QUIZ_TYPE = "short_answer";
    private static final String OX_QUIZ_TYPE = "ox";

    private final RestClient restClient;

    /** AI 서버 전용 RestClient 주입 */
    public AiQuizClient(@Qualifier("aiRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    /**
     * 단답형 리콜 퀴즈 생성 요청
     *
     * @param request AI 리콜 퀴즈 생성 요청
     * @return AI 단답형 리콜 퀴즈 생성 응답
     */
    @Retryable(
            retryFor = {RestClientException.class},
            noRetryFor = {BusinessException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2.0)
    )
    public AiShortAnswerQuizResponse generateShortAnswerQuiz(AiQuizRequest request) {
        try {
            AiShortAnswerQuizResponse response = restClient.post()
                    .uri("/ai/quiz")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(AiShortAnswerQuizResponse.class);

            validateShortAnswerResponse(request, response);
            return response;
        } catch (HttpClientErrorException e) {
            throw new BusinessException(AiErrorCode.AI_QUIZ_GENERATION_FAILED);
        }
    }

    /**
     * OX 리콜 퀴즈 생성 요청
     *
     * @param request AI 리콜 퀴즈 생성 요청
     * @return AI OX 리콜 퀴즈 생성 응답
     */
    @Retryable(
            retryFor = {RestClientException.class},
            noRetryFor = {BusinessException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2.0)
    )
    public AiOxQuizResponse generateOxQuiz(AiQuizRequest request) {
        try {
            AiOxQuizResponse response = restClient.post()
                    .uri("/ai/quiz")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(AiOxQuizResponse.class);

            validateOxResponse(request, response);
            return response;
        } catch (HttpClientErrorException e) {
            throw new BusinessException(AiErrorCode.AI_QUIZ_GENERATION_FAILED);
        }
    }

    /**
     * 단답형 리콜 퀴즈 생성 재시도 실패 처리
     *
     * @param e 마지막으로 발생한 예외
     * @param request AI 리콜 퀴즈 생성 요청
     * @return 재시도 성공 시 응답
     */
    @Recover
    public AiShortAnswerQuizResponse recoverGenerateShortAnswerQuiz(Exception e, AiQuizRequest request) {
        if (e instanceof BusinessException be) {
            throw be;
        }
        log.error("AI short answer quiz generation failed after all retries: {}", e.getMessage(), e);
        throw new BusinessException(AiErrorCode.AI_QUIZ_GENERATION_FAILED);
    }

    /**
     * OX 리콜 퀴즈 생성 재시도 실패 처리
     *
     * @param e 마지막으로 발생한 예외
     * @param request AI 리콜 퀴즈 생성 요청
     * @return 재시도 성공 시 응답
     */
    @Recover
    public AiOxQuizResponse recoverGenerateOxQuiz(Exception e, AiQuizRequest request) {
        if (e instanceof BusinessException be) {
            throw be;
        }
        log.error("AI OX quiz generation failed after all retries: {}", e.getMessage(), e);
        throw new BusinessException(AiErrorCode.AI_QUIZ_GENERATION_FAILED);
    }

    /** AI 단답형 리콜 퀴즈 생성 응답 검증 */
    private void validateShortAnswerResponse(AiQuizRequest request, AiShortAnswerQuizResponse response) {
        if (response == null
                || !SHORT_ANSWER_QUIZ_TYPE.equals(response.quizType())
                || hasInvalidQuestionCount(request, response.questions())
                || response.questions().stream().anyMatch(this::isInvalidShortAnswerQuestion)) {
            throw new BusinessException(AiErrorCode.AI_QUIZ_INVALID_RESPONSE);
        }
    }

    /** AI OX 리콜 퀴즈 생성 응답 검증 */
    private void validateOxResponse(AiQuizRequest request, AiOxQuizResponse response) {
        if (response == null
                || !OX_QUIZ_TYPE.equals(response.quizType())
                || hasInvalidQuestionCount(request, response.questions())
                || response.questions().stream().anyMatch(this::isInvalidOxQuestion)) {
            throw new BusinessException(AiErrorCode.AI_QUIZ_INVALID_RESPONSE);
        }
    }

    /** 단답형 문항 필수값 검증 */
    private boolean isInvalidShortAnswerQuestion(AiShortAnswerQuizQuestionResponse question) {
        return question == null
                || isBlank(question.question())
                || isBlank(question.answer());
    }

    /** OX 문항 필수값 검증 */
    private boolean isInvalidOxQuestion(AiOxQuizQuestionResponse question) {
        return question == null
                || isBlank(question.statement())
                || question.isCorrect() == null;
    }

    /** 문항 개수 검증 */
    private boolean hasInvalidQuestionCount(AiQuizRequest request, List<?> questions) {
        return request == null
                || request.contents() == null
                || questions == null
                || questions.isEmpty()
                || questions.size() != request.contents().size();
    }

    /** 빈 문자열 확인 */
    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
