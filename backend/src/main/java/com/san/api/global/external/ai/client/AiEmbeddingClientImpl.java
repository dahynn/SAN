package com.san.api.global.external.ai.client;

import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.CommonErrorCode;
import com.san.api.global.external.ai.dto.request.AiEmbedRequest;
import com.san.api.global.external.ai.dto.response.AiEmbedResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * AI 서버(FastAPI) 임베딩 API 호출 구현체.
 * POST /ai/search 호출 후 1536차원 임베딩 벡터를 반환한다.
 * AI 서버 장애 또는 통신 오류 발생 시 BusinessException(EXTERNAL_API_ERROR)을 던진다.
 */
@Slf4j
@Component
public class AiEmbeddingClientImpl implements AiEmbeddingClient {

    private final RestClient restClient;

    /** 공통 설정이 적용된 AI 서버 호출 Client를 주입받는다. */
    public AiEmbeddingClientImpl(@Qualifier("aiRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    /**
     * 텍스트를 AI 서버에 전달하여 1536차원 임베딩 벡터로 변환한다.
     *
     * @param text 임베딩할 텍스트
     * @return 1536차원 float 배열
     * @throws BusinessException AI 서버 장애 또는 통신 오류 발생 시
     */
    @Override
    public float[] embed(String text) {
        try {
            AiEmbedResponse response = restClient.post()
                    .uri("/ai/search")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(new AiEmbedRequest(text))
                    .retrieve()
                    .body(AiEmbedResponse.class);

            if (response == null || response.embedding() == null) {
                throw new BusinessException(CommonErrorCode.EXTERNAL_API_ERROR);
            }

            float[] vector = new float[response.embedding().size()];
            for (int i = 0; i < vector.length; i++) {
                vector[i] = response.embedding().get(i);
            }
            return vector;
        } catch (RestClientException e) {
            log.error("AI embedding request failed: {}", e.getMessage(), e);
            throw new BusinessException(CommonErrorCode.EXTERNAL_API_ERROR);
        }
    }
}
