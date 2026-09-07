package com.san.api.global.config;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.TimeZone;

/** 애플리케이션 날짜 생성 기준을 한국 시간으로 고정. */
@Configuration
public class TimeZoneConfig {

    public static final ZoneId APPLICATION_ZONE_ID = ZoneId.of("Asia/Seoul");

    @PostConstruct
    public void setDefaultTimeZone() {
        TimeZone.setDefault(TimeZone.getTimeZone(APPLICATION_ZONE_ID));
    }

    @Bean
    public Clock applicationClock() {
        return Clock.system(APPLICATION_ZONE_ID);
    }

    @Bean
    public DateTimeProvider dateTimeProvider(Clock applicationClock) {
        return () -> Optional.of(LocalDateTime.now(applicationClock));
    }
}
