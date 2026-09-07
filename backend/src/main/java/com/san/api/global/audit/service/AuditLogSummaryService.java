package com.san.api.global.audit.service;

import com.san.api.global.audit.dto.response.AuditLogResponse;
import com.san.api.global.audit.dto.response.AuditLogSummaryResponse;
import com.san.api.global.audit.entity.AuditEventDomain;
import com.san.api.global.audit.entity.AuditEventType;
import com.san.api.global.audit.entity.AuditOutcome;
import com.san.api.global.audit.repository.AuditLogEventRepository;
import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.CommonErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuditLogSummaryService {

    private static final int DEFAULT_LOOKBACK_HOURS = 6;
    private static final int TOP_FAILURE_REASON_LIMIT = 5;
    private static final int RECENT_FAILURE_LIMIT = 10;

    private final AuditLogEventRepository auditLogEventRepository;
    private final Clock applicationClock;

    /**
     * 관리자 운영 화면에서 사용할 감사 로그 요약 지표를 조회합니다.
     *
     * @param from 조회 시작 시각, 없으면 현재 시각 기준 6시간 전
     * @param to 조회 종료 시각, 없으면 현재 시각
     * @return 감사 로그 성공/실패율, 도메인별 건수, 비동기 작업 지표, 최근 실패 로그
     */
    @Transactional(readOnly = true)
    public AuditLogSummaryResponse summarize(LocalDateTime from, LocalDateTime to) {
        TimeRange timeRange = resolveTimeRange(from, to);
        long totalCount = auditLogEventRepository.countByOccurredAtBetween(timeRange.from(), timeRange.to());
        Map<AuditOutcome, Long> outcomeCounts = outcomeCounts(timeRange);
        long successCount = outcomeCounts.getOrDefault(AuditOutcome.SUCCESS, 0L);
        long failureCount = outcomeCounts.getOrDefault(AuditOutcome.FAILURE, 0L);

        return new AuditLogSummaryResponse(
                timeRange.from(),
                timeRange.to(),
                totalCount,
                successCount,
                failureCount,
                percentage(failureCount, totalCount),
                domainCounts(timeRange),
                asyncJobSummary(timeRange),
                topFailureReasons(timeRange),
                recentFailures(timeRange)
        );
    }

    private TimeRange resolveTimeRange(LocalDateTime from, LocalDateTime to) {
        LocalDateTime resolvedTo = to == null ? LocalDateTime.now(applicationClock) : to;
        LocalDateTime resolvedFrom = from == null ? resolvedTo.minusHours(DEFAULT_LOOKBACK_HOURS) : from;
        if (resolvedFrom.isAfter(resolvedTo)) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT_VALUE, "감사 로그 조회 시작 시각은 종료 시각보다 늦을 수 없습니다.");
        }
        return new TimeRange(resolvedFrom, resolvedTo);
    }

    private Map<AuditOutcome, Long> outcomeCounts(TimeRange timeRange) {
        Map<AuditOutcome, Long> counts = new EnumMap<>(AuditOutcome.class);
        auditLogEventRepository.countByOutcomeBetween(timeRange.from(), timeRange.to())
                .forEach(row -> counts.put(row.getOutcome(), row.getCount()));
        return counts;
    }

    private List<AuditLogSummaryResponse.DomainCountResponse> domainCounts(TimeRange timeRange) {
        return auditLogEventRepository.countByEventDomainBetween(timeRange.from(), timeRange.to()).stream()
                .map(row -> new AuditLogSummaryResponse.DomainCountResponse(row.getEventDomain(), row.getCount()))
                .toList();
    }

    private AuditLogSummaryResponse.AsyncJobSummaryResponse asyncJobSummary(TimeRange timeRange) {
        Map<AuditEventType, Long> counts = new EnumMap<>(AuditEventType.class);
        auditLogEventRepository.countByEventTypeInDomainBetween(
                        AuditEventDomain.ASYNC_JOB,
                        timeRange.from(),
                        timeRange.to()
                )
                .forEach(row -> counts.put(row.getEventType(), row.getCount()));

        long processingCount = counts.getOrDefault(AuditEventType.ASYNC_JOB_PROCESSING, 0L);
        long succeededCount = counts.getOrDefault(AuditEventType.ASYNC_JOB_SUCCEEDED, 0L);
        long failedCount = counts.getOrDefault(AuditEventType.ASYNC_JOB_FAILED, 0L);
        long completedCount = succeededCount + failedCount;
        return new AuditLogSummaryResponse.AsyncJobSummaryResponse(
                processingCount,
                succeededCount,
                failedCount,
                completedCount,
                percentage(failedCount, completedCount)
        );
    }

    private List<AuditLogSummaryResponse.FailureReasonCountResponse> topFailureReasons(TimeRange timeRange) {
        return auditLogEventRepository.findTopFailureReasonsBetween(
                        AuditOutcome.FAILURE,
                        timeRange.from(),
                        timeRange.to(),
                        PageRequest.of(0, TOP_FAILURE_REASON_LIMIT)
                ).stream()
                .map(row -> new AuditLogSummaryResponse.FailureReasonCountResponse(
                        row.getFailureReasonCode(),
                        row.getCount()
                ))
                .toList();
    }

    private List<AuditLogResponse> recentFailures(TimeRange timeRange) {
        return auditLogEventRepository.findByOutcomeAndOccurredAtBetweenOrderByOccurredAtDesc(
                        AuditOutcome.FAILURE,
                        timeRange.from(),
                        timeRange.to(),
                        PageRequest.of(0, RECENT_FAILURE_LIMIT)
                ).stream()
                .map(AuditLogResponse::from)
                .toList();
    }

    private BigDecimal percentage(long numerator, long denominator) {
        if (denominator == 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return BigDecimal.valueOf(numerator)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
    }

    private record TimeRange(LocalDateTime from, LocalDateTime to) {
    }
}
