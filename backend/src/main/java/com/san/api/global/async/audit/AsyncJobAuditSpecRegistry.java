package com.san.api.global.async.audit;

import com.san.api.global.async.entity.JobType;
import com.san.api.global.audit.entity.AuditEventDomain;
import com.san.api.global.audit.entity.AuditEventType;
import com.san.api.global.audit.entity.AuditTargetType;
import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.CommonErrorCode;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

@Component
public class AsyncJobAuditSpecRegistry {

    private final Map<JobType, AsyncJobAuditSpec> specs = new EnumMap<>(JobType.class);

    public AsyncJobAuditSpecRegistry() {
        for (JobType jobType : JobType.values()) {
            specs.put(jobType, asyncJobSpec(jobType));
        }
    }

    /**
     * 비동기 작업 유형에 대응하는 운영 감사 로그 스펙을 조회합니다.
     *
     * @param jobType 감사 로그를 기록할 비동기 작업 유형
     * @return 비동기 작업 상태 전이에 사용할 감사 로그 스펙
     */
    public AsyncJobAuditSpec get(JobType jobType) {
        AsyncJobAuditSpec spec = specs.get(jobType);
        if (spec == null) {
            throw new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR, "비동기 작업 감사 스펙을 찾을 수 없습니다.");
        }
        return spec;
    }

    private AsyncJobAuditSpec asyncJobSpec(JobType jobType) {
        return new AsyncJobAuditSpec(
                jobType,
                AuditEventDomain.ASYNC_JOB,
                AuditTargetType.ASYNC_JOB,
                AuditEventType.ASYNC_JOB_PROCESSING,
                AuditEventType.ASYNC_JOB_SUCCEEDED,
                AuditEventType.ASYNC_JOB_FAILED
        );
    }
}
