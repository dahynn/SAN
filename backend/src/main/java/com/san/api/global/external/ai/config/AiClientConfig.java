package com.san.api.global.external.ai.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/** AI 서버 HTTP Client 설정 */
@Configuration
public class AiClientConfig {

    /** AI 서버 호출에 공통으로 사용할 RestClient를 생성한다. */
    @Bean
    public RestClient aiRestClient(
            @Value("${ai.server.base-url}") String aiServerUrl,
            @Value("${ai.server.connect-timeout-millis}") int connectTimeoutMillis,
            @Value("${ai.server.read-timeout-millis}") int readTimeoutMillis
    ) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeoutMillis);
        requestFactory.setReadTimeout(readTimeoutMillis);

        return RestClient.builder()
                .baseUrl(aiServerUrl)
                .requestFactory(requestFactory)
                .build();
    }
}
