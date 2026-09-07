package com.san.api.global.async.audit;

import com.san.api.global.async.entity.JobType;
import com.san.api.global.audit.entity.AuditEventDomain;
import com.san.api.global.audit.entity.AuditEventType;
import com.san.api.global.audit.entity.AuditTargetType;

import java.util.Objects;

public record AsyncJobAuditSpec(
        JobType jobType,
        AuditEventDomain eventDomain,
        AuditTargetType targetType,
        AuditEventType processingEventType,
        AuditEventType succeededEventType,
        AuditEventType failedEventType
) {

    public AsyncJobAuditSpec {
        Objects.requireNonNull(jobType, "작업 유형은 필수입니다.");
        Objects.requireNonNull(eventDomain, "감사 이벤트 도메인은 필수입니다.");
        Objects.requireNonNull(targetType, "감사 대상 유형은 필수입니다.");
        Objects.requireNonNull(processingEventType, "처리 중 감사 이벤트 유형은 필수입니다.");
        Objects.requireNonNull(succeededEventType, "성공 감사 이벤트 유형은 필수입니다.");
        Objects.requireNonNull(failedEventType, "실패 감사 이벤트 유형은 필수입니다.");
    }
}
