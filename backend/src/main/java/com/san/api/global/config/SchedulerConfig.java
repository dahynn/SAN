package com.san.api.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 스케줄러 활성화.
 *
 * @Scheduled 어노테이션을 사용하는 주기적 작업(알림, 만료 처리 등)이 실행되려면 반드시 필요.
 */
@Configuration
@EnableScheduling
public class SchedulerConfig {
}
