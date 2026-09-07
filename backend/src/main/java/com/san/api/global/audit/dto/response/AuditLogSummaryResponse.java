package com.san.api.global.audit.dto.response;

import com.san.api.global.audit.entity.AuditEventDomain;
import com.san.api.global.audit.entity.AuditEventType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record AuditLogSummaryResponse(
        LocalDateTime from,
        LocalDateTime to,
        long totalCount,
        long successCount,
        long failureCount,
        BigDecimal failureRate,
        List<DomainCountResponse> domainCounts,
        AsyncJobSummaryResponse asyncJob,
        List<FailureReasonCountResponse> topFailureReasons,
        List<AuditLogResponse> recentFailures
) {

    public record DomainCountResponse(
            AuditEventDomain eventDomain,
            long count
    ) {
    }

    public record AsyncJobSummaryResponse(
            long processingCount,
            long succeededCount,
            long failedCount,
            long completedCount,
            BigDecimal failureRate
    ) {
    }

    public record FailureReasonCountResponse(
            String failureReasonCode,
            long count
    ) {
    }
}
