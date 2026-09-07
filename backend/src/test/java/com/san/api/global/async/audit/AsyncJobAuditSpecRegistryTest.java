package com.san.api.global.async.audit;

import com.san.api.global.async.entity.JobType;
import com.san.api.global.audit.entity.AuditEventDomain;
import com.san.api.global.audit.entity.AuditEventType;
import com.san.api.global.audit.entity.AuditTargetType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AsyncJobAuditSpecRegistryTest {

    @Test
    void 모든_비동기_작업_유형에_운영_감사_스펙을_제공한다() {
        AsyncJobAuditSpecRegistry registry = new AsyncJobAuditSpecRegistry();

        for (JobType jobType : JobType.values()) {
            AsyncJobAuditSpec spec = registry.get(jobType);

            assertThat(spec.jobType()).isEqualTo(jobType);
            assertThat(spec.eventDomain()).isEqualTo(AuditEventDomain.ASYNC_JOB);
            assertThat(spec.targetType()).isEqualTo(AuditTargetType.ASYNC_JOB);
            assertThat(spec.processingEventType()).isEqualTo(AuditEventType.ASYNC_JOB_PROCESSING);
            assertThat(spec.succeededEventType()).isEqualTo(AuditEventType.ASYNC_JOB_SUCCEEDED);
            assertThat(spec.failedEventType()).isEqualTo(AuditEventType.ASYNC_JOB_FAILED);
        }
    }
}
