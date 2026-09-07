package com.san.api.global.config;

import org.junit.jupiter.api.Test;
import org.springframework.data.auditing.DateTimeProvider;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;

class TimeZoneConfigTest {

    private final TimeZoneConfig timeZoneConfig = new TimeZoneConfig();

    @Test
    void setDefaultTimeZone_setsAsiaSeoul() {
        TimeZone originalTimeZone = TimeZone.getDefault();

        try {
            timeZoneConfig.setDefaultTimeZone();

            assertThat(TimeZone.getDefault().getID()).isEqualTo("Asia/Seoul");
        } finally {
            TimeZone.setDefault(originalTimeZone);
        }
    }

    @Test
    void dateTimeProvider_returnsAsiaSeoulLocalDateTime() {
        Clock applicationClock = timeZoneConfig.applicationClock();
        DateTimeProvider dateTimeProvider = timeZoneConfig.dateTimeProvider(applicationClock);
        LocalDateTime before = LocalDateTime.now(TimeZoneConfig.APPLICATION_ZONE_ID);

        LocalDateTime providedDateTime = LocalDateTime.from(dateTimeProvider.getNow().orElseThrow());
        LocalDateTime after = LocalDateTime.now(TimeZoneConfig.APPLICATION_ZONE_ID);

        assertThat(providedDateTime).isBetween(before, after);
    }
}
