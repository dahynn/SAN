package com.san.api.global.external.ai.client;

import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.AiErrorCode;
import com.san.api.global.external.ai.dto.request.AiTilRequest;
import com.san.api.global.external.ai.dto.response.AiTilResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** AI TIL 생성 Client */
@Slf4j
@Component
public class AiTilClient {

    private final RestClient restClient;

    /** 공통 설정이 적용된 AI 서버 호출 Client를 주입받는다. */
    public AiTilClient(@Qualifier("aiRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    /**
     * AI 서버에 TIL 생성 요청
     *
     * @param request AI TIL 생성 요청
     * @return AI TIL 생성 응답
     */
    @Retryable(
            retryFor = {RestClientException.class},
            noRetryFor = {BusinessException.class, HttpClientErrorException.class},
            maxAttempts = 1
    )
    public AiTilResponse generateTil(AiTilRequest request) {
        AiTilResponse response = restClient.post()
                .uri("/ai/til")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(AiTilResponse.class);

        validateResponse(response);
        return response;
    }

    /**
     * 재시도 소진 후 호출되는 복구 메서드.
     * BusinessException(응답 검증 실패)은 그대로 전파하고, RestClientException은 도메인 예외로 변환한다.
     *
     * @param e 마지막 시도에서 발생한 예외
     * @param request 원본 요청 (시그니처 매칭용)
     */
    @Recover
    public AiTilResponse recoverGenerateTil(Exception e, AiTilRequest request) {
        if (e instanceof BusinessException be) {
            throw be;
        }
        log.error("AI TIL generation failed after all retries: {}", e.getMessage(), e);
        throw new BusinessException(AiErrorCode.AI_TIL_GENERATION_FAILED);
    }

    /**
     * AI TIL 생성 응답 필수값 검증
     *
     * @param response AI TIL 생성 응답
     */
    private void validateResponse(AiTilResponse response) {
        if (response == null
                || isBlank(response.tilMarkdown())
                || response.embedding() == null
                || response.embedding().length == 0) {
            throw new BusinessException(AiErrorCode.AI_TIL_INVALID_RESPONSE);
        }
    }

    /** 빈 문자열 여부 확인 */
    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
