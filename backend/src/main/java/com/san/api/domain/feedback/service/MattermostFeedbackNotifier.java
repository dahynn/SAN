package com.san.api.domain.feedback.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;

/** 등록된 피드백을 Mattermost Incoming Webhook으로 알립니다. */
@Slf4j
@Component
public class MattermostFeedbackNotifier {

    private final RestClient restClient;
    private final String webhookUrl;
    private final int maxAttempts;

    public MattermostFeedbackNotifier(
            RestClient.Builder restClientBuilder,
            @Value("${feedback.mattermost.webhook-url:}") String webhookUrl,
            @Value("${feedback.mattermost.max-attempts:3}") int maxAttempts) {
        this.restClient = restClientBuilder.build();
        this.webhookUrl = webhookUrl;
        this.maxAttempts = Math.max(1, maxAttempts);
    }

    /**
     * Mattermost webhook으로 피드백 알림을 동기 전송합니다.
     *
     * @param payload Mattermost 알림에 필요한 피드백 스냅샷
     * @throws RestClientException 최대 재시도 후에도 전송에 실패한 경우
     */
    public void send(FeedbackNotificationPayload payload) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            throw new ResourceAccessException("Mattermost webhook URL is not configured");
        }

        RestClientException lastException = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                restClient.post()
                        .uri(webhookUrl)
                        .body(Map.of("text", createMessage(payload)))
                        .retrieve()
                        .toBodilessEntity();
                return;
            } catch (RestClientException e) {
                lastException = e;
                log.warn("Failed to send feedback notification to Mattermost. feedbackId={}, attempt={}/{}",
                        payload.feedbackId(), attempt, maxAttempts, e);
            }
        }

        throw lastException;
    }

    /** Mattermost 채널에 표시할 피드백 메시지를 생성합니다. */
    private String createMessage(FeedbackNotificationPayload payload) {
        return """
                ## 📬 NEW Feedback 배송왔습니다
                | **항목** | **내용** |
                | --- | --- |
                | **유형** | `%s` |
                | **작성자** | `%s` |
                | **클라이언트** | `%s` |
                | **연락처** | %s |
                | **작성 위치** | %s |
                | **추적 ID** | `%s` |
                | **피드백 ID** | `%s` |

                ### 내용
                ```
                %s
                ```
                """.formatted(
                payload.type(),
                valueOrDash(payload.userId()),
                valueOrDash(payload.clientType()),
                valueOrDash(payload.contact()),
                valueOrDash(payload.pageUrl()),
                valueOrDash(payload.traceId()),
                payload.feedbackId(),
                payload.content()
        );
    }

    /** 알림 메시지에서 비어 있는 값을 '-'로 표시합니다. */
    private String valueOrDash(Object value) {
        if (value == null || value.toString().isBlank()) {
            return "-";
        }
        return value.toString();
    }
}
