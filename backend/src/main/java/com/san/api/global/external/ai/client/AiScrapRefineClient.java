package com.san.api.global.external.ai.client;

import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.AiErrorCode;
import com.san.api.global.external.ai.dto.request.AiScrapRefineRequest;
import com.san.api.global.external.ai.dto.response.AiScrapRefineResponse;
import com.san.api.global.external.ai.dto.response.AiTilResponse;
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

/** AI 원본 정제 Client */
@Slf4j
@Component
public class AiScrapRefineClient {

    private final RestClient restClient;

    /** 공통 설정을 기반으로 AI 서버 호출 Client를 주입받는다. */
    public AiScrapRefineClient(@Qualifier("aiRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    /**
     * AI 서버에 원본 정제를 요청
     *
     * @param request AI 원본 정제 요청
     * @return AI 원본 정제 응답
     */
    @Retryable(
            retryFor = {RestClientException.class},
            noRetryFor = {BusinessException.class, HttpClientErrorException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2.0)
    )
    public AiScrapRefineResponse refine(AiScrapRefineRequest request) {
        AiTilResponse response = restClient.post()
                .uri("/ai/til")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(request.toTilRequest())
                .retrieve()
                .body(AiTilResponse.class);

        validateResponse(response);
        return AiScrapRefineResponse.from(response);
    }

    /**
     * 모든 재시도 실패 시 원본 정제 실패 예외로 변환
     *
     * @param e       재시도 과정에서 발생한 예외
     * @param request 원본 정제 요청
     * @return 사용하지 않음
     */
    @Recover
    public AiScrapRefineResponse recoverRefine(Exception e, AiScrapRefineRequest request) {
        if (e instanceof BusinessException be) {
            throw be;
        }
        log.error("AI scrap refine failed after all retries: {}", e.getMessage(), e);
        throw new BusinessException(AiErrorCode.AI_SCRAP_REFINE_FAILED);
    }

    /**
     * AI 원본 정제 응답 필수값 검증
     *
     * @param response AI TIL 응답
     */
    private void validateResponse(AiTilResponse response) {
        if (response == null || isBlank(response.tilMarkdown())) {
            throw new BusinessException(AiErrorCode.AI_SCRAP_REFINE_INVALID_RESPONSE);
        }
    }

    /** 빈 문자열 여부 확인 */
    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
