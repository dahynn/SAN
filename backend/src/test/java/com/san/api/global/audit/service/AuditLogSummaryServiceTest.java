package com.san.api.global.audit.service;

import com.san.api.global.audit.dto.response.AuditLogSummaryResponse;
import com.san.api.global.audit.entity.AuditEventDomain;
import com.san.api.global.audit.entity.AuditEventType;
import com.san.api.global.audit.entity.AuditLogEvent;
import com.san.api.global.audit.entity.AuditOutcome;
import com.san.api.global.audit.repository.AuditLogEventRepository;
import com.san.api.global.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditLogSummaryServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-05-27T06:00:00Z"),
            ZoneId.of("Asia/Seoul")
    );

    @Mock
    private AuditLogEventRepository auditLogEventRepository;

    @Test
    void summarizeUsesLastSixHoursAsDefaultRangeAndAggregatesOperationalMetrics() {
        AuditLogSummaryService service = new AuditLogSummaryService(auditLogEventRepository, FIXED_CLOCK);
        LocalDateTime expectedTo = LocalDateTime.of(2026, 5, 27, 15, 0);
        LocalDateTime expectedFrom = LocalDateTime.of(2026, 5, 27, 9, 0);
        AuditLogEvent recentFailure = failureEvent(expectedTo.minusMinutes(10));
        List<AuditLogEventRepository.OutcomeCountProjection> outcomeCounts = List.of(
                outcomeCount(AuditOutcome.SUCCESS, 92),
                outcomeCount(AuditOutcome.FAILURE, 8)
        );
        List<AuditLogEventRepository.DomainCountProjection> domainCounts = List.of(
                domainCount(AuditEventDomain.AUTH, 60),
                domainCount(AuditEventDomain.ASYNC_JOB, 20)
        );
        List<AuditLogEventRepository.EventTypeCountProjection> asyncJobEventCounts = List.of(
                eventTypeCount(AuditEventType.ASYNC_JOB_PROCESSING, 20),
                eventTypeCount(AuditEventType.ASYNC_JOB_SUCCEEDED, 18),
                eventTypeCount(AuditEventType.ASYNC_JOB_FAILED, 2)
        );
        List<AuditLogEventRepository.FailureReasonCountProjection> topFailureReasons = List.of(
                failureReasonCount("A202", 4)
        );

        when(auditLogEventRepository.countByOccurredAtBetween(expectedFrom, expectedTo)).thenReturn(100L);
        when(auditLogEventRepository.countByOutcomeBetween(expectedFrom, expectedTo)).thenReturn(outcomeCounts);
        when(auditLogEventRepository.countByEventDomainBetween(expectedFrom, expectedTo)).thenReturn(domainCounts);
        when(auditLogEventRepository.countByEventTypeInDomainBetween(
                AuditEventDomain.ASYNC_JOB,
                expectedFrom,
                expectedTo
        )).thenReturn(asyncJobEventCounts);
        when(auditLogEventRepository.findTopFailureReasonsBetween(
                org.mockito.ArgumentMatchers.eq(AuditOutcome.FAILURE),
                org.mockito.ArgumentMatchers.eq(expectedFrom),
                org.mockito.ArgumentMatchers.eq(expectedTo),
                any(Pageable.class)
        )).thenReturn(topFailureReasons);
        when(auditLogEventRepository.findByOutcomeAndOccurredAtBetweenOrderByOccurredAtDesc(
                org.mockito.ArgumentMatchers.eq(AuditOutcome.FAILURE),
                org.mockito.ArgumentMatchers.eq(expectedFrom),
                org.mockito.ArgumentMatchers.eq(expectedTo),
                any(Pageable.class)
        )).thenReturn(List.of(recentFailure));

        AuditLogSummaryResponse response = service.summarize(null, null);

        assertThat(response.from()).isEqualTo(expectedFrom);
        assertThat(response.to()).isEqualTo(expectedTo);
        assertThat(response.totalCount()).isEqualTo(100);
        assertThat(response.successCount()).isEqualTo(92);
        assertThat(response.failureCount()).isEqualTo(8);
        assertThat(response.failureRate()).isEqualByComparingTo(new BigDecimal("8.00"));
        assertThat(response.domainCounts())
                .extracting(AuditLogSummaryResponse.DomainCountResponse::eventDomain)
                .containsExactly(AuditEventDomain.AUTH, AuditEventDomain.ASYNC_JOB);
        assertThat(response.asyncJob().processingCount()).isEqualTo(20);
        assertThat(response.asyncJob().succeededCount()).isEqualTo(18);
        assertThat(response.asyncJob().failedCount()).isEqualTo(2);
        assertThat(response.asyncJob().completedCount()).isEqualTo(20);
        assertThat(response.asyncJob().failureRate()).isEqualByComparingTo(new BigDecimal("10.00"));
        assertThat(response.topFailureReasons().get(0).failureReasonCode()).isEqualTo("A202");
        assertThat(response.recentFailures()).hasSize(1);

        ArgumentCaptor<Pageable> topFailurePageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(auditLogEventRepository).findTopFailureReasonsBetween(
                org.mockito.ArgumentMatchers.eq(AuditOutcome.FAILURE),
                org.mockito.ArgumentMatchers.eq(expectedFrom),
                org.mockito.ArgumentMatchers.eq(expectedTo),
                topFailurePageableCaptor.capture()
        );
        assertThat(topFailurePageableCaptor.getValue().getPageSize()).isEqualTo(5);

        ArgumentCaptor<Pageable> recentFailurePageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(auditLogEventRepository).findByOutcomeAndOccurredAtBetweenOrderByOccurredAtDesc(
                org.mockito.ArgumentMatchers.eq(AuditOutcome.FAILURE),
                org.mockito.ArgumentMatchers.eq(expectedFrom),
                org.mockito.ArgumentMatchers.eq(expectedTo),
                recentFailurePageableCaptor.capture()
        );
        assertThat(recentFailurePageableCaptor.getValue().getPageSize()).isEqualTo(10);
    }

    @Test
    void summarizeRejectsInvalidRange() {
        AuditLogSummaryService service = new AuditLogSummaryService(auditLogEventRepository, FIXED_CLOCK);

        assertThatThrownBy(() -> service.summarize(
                LocalDateTime.of(2026, 5, 27, 16, 0),
                LocalDateTime.of(2026, 5, 27, 15, 0)
        )).isInstanceOf(BusinessException.class);
    }

    private AuditLogEventRepository.OutcomeCountProjection outcomeCount(AuditOutcome outcome, long count) {
        AuditLogEventRepository.OutcomeCountProjection projection = mock(AuditLogEventRepository.OutcomeCountProjection.class);
        when(projection.getOutcome()).thenReturn(outcome);
        when(projection.getCount()).thenReturn(count);
        return projection;
    }

    private AuditLogEventRepository.DomainCountProjection domainCount(AuditEventDomain eventDomain, long count) {
        AuditLogEventRepository.DomainCountProjection projection = mock(AuditLogEventRepository.DomainCountProjection.class);
        when(projection.getEventDomain()).thenReturn(eventDomain);
        when(projection.getCount()).thenReturn(count);
        return projection;
    }

    private AuditLogEventRepository.EventTypeCountProjection eventTypeCount(AuditEventType eventType, long count) {
        AuditLogEventRepository.EventTypeCountProjection projection = mock(AuditLogEventRepository.EventTypeCountProjection.class);
        when(projection.getEventType()).thenReturn(eventType);
        when(projection.getCount()).thenReturn(count);
        return projection;
    }

    private AuditLogEventRepository.FailureReasonCountProjection failureReasonCount(String failureReasonCode, long count) {
        AuditLogEventRepository.FailureReasonCountProjection projection = mock(AuditLogEventRepository.FailureReasonCountProjection.class);
        when(projection.getFailureReasonCode()).thenReturn(failureReasonCode);
        when(projection.getCount()).thenReturn(count);
        return projection;
    }

    private AuditLogEvent failureEvent(LocalDateTime occurredAt) {
        AuditLogEvent event = AuditLogEvent.builder()
                .traceId("trace-340")
                .eventDomain(AuditEventDomain.AUTH)
                .eventType(AuditEventType.LOGIN_FAILURE)
                .targetType("USER")
                .outcome(AuditOutcome.FAILURE)
                .failureReasonCode("A202")
                .failureMessage("GitHub 계정이 연결되어 있지 않습니다.")
                .metadata(Map.of("source", "summary-test"))
                .build();
        ReflectionTestUtils.setField(event, "occurredAt", occurredAt);
        return event;
    }
}
