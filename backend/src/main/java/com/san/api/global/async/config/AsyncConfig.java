package com.san.api.global.async.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 비동기 실행 활성화 및 잡 유형별 전용 스레드 풀 설정.
 *
 * <p>AI 호출 잡, 외부 API 잡, 알림 잡을 각각 독립된 풀로 분리하여
 * AI 잡이 몰릴 때 GitHub 커밋·알림 처리가 지연되지 않도록 합니다.</p>
 */
@EnableAsync(proxyTargetClass = true)
@Configuration
public class AsyncConfig {

    /**
     * AI 호출 잡 전용 스레드 풀.
     * 대상: CARD_ANALYSIS, TIL_GENERATION, SCRAP_REFINE
     */
    @Bean(name = "aiJobExecutor")
    public Executor aiJobExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("ai-job-");
        executor.initialize();
        return executor;
    }

    /**
     * 외부 API 잡 전용 스레드 풀.
     * 대상: TIL_GITHUB_COMMIT
     */
    @Bean(name = "githubJobExecutor")
    public Executor githubJobExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(6);
        executor.setQueueCapacity(30);
        executor.setThreadNamePrefix("github-job-");
        executor.initialize();
        return executor;
    }

    /**
     * 알림 전용 스레드 풀.
     * 대상: MattermostFeedbackNotifier
     */
    @Bean(name = "notificationExecutor")
    public Executor notificationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(3);
        executor.setQueueCapacity(20);
        executor.setThreadNamePrefix("notification-");
        executor.initialize();
        return executor;
    }
}
