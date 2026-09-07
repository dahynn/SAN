package com.san.api.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/** JPA Auditing 활성화. 슬라이스 테스트 충돌 방지를 위해 메인 클래스와 분리. */
@Configuration
@EnableJpaAuditing(dateTimeProviderRef = "dateTimeProvider")
public class JpaConfig {
}
