package com.san.api.global.async.entity;

import com.san.api.global.audit.context.AuditContextSnapshot;
import com.san.api.global.audit.context.AuditRequesterType;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AsyncJobTest {

    @Test
    void createsPendingJobWithAuditRequestContextSnapshot() {
        UUID actorUserId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("source", "api");

        AsyncJob job = AsyncJob.builder()
                .jobType(JobType.SCRAP_REFINE)
                .targetId(targetId)
                .auditContext(AuditContextSnapshot.builder()
                        .actorUserId(actorUserId)
                        .traceId("trace-1")
                        .ipAddress("203.0.113.10")
                        .userAgent("JUnit")
                        .requestedByType(AuditRequesterType.USER)
                        .requestMetadata(metadata)
                        .build())
                .build();

        metadata.put("source", "changed");

        assertThat(job.getStatus()).isEqualTo(JobStatus.PENDING);
        assertThat(job.getJobType()).isEqualTo(JobType.SCRAP_REFINE);
        assertThat(job.getTargetId()).isEqualTo(targetId);
        assertThat(job.getAuditContext().getActorUserId()).isEqualTo(actorUserId);
        assertThat(job.getAuditContext().getTraceId()).isEqualTo("trace-1");
        assertThat(job.getAuditContext().getIpAddress()).isEqualTo("203.0.113.10");
        assertThat(job.getAuditContext().getUserAgent()).isEqualTo("JUnit");
        assertThat(job.getAuditContext().getRequestedByType()).isEqualTo(AuditRequesterType.USER);
        assertThat(job.getAuditContext().getRequestMetadata()).containsEntry("source", "api");
    }

    @Test
    void recordsExecutionTimestampsOnStatusTransitions() {
        AsyncJob job = AsyncJob.builder()
                .jobType(JobType.CARD_ANALYSIS)
                .targetId(UUID.randomUUID())
                .build();

        job.updateStatus(JobStatus.PROCESSING);
        job.updateStatus(JobStatus.COMPLETED);

        assertThat(job.getStartedAt()).isNotNull();
        assertThat(job.getCompletedAt()).isNotNull();
    }

    @Test
    void recordsCompletedAtWhenJobFails() {
        AsyncJob job = AsyncJob.builder()
                .jobType(JobType.CARD_ANALYSIS)
                .targetId(UUID.randomUUID())
                .build();

        job.fail("failed");

        assertThat(job.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(job.getErrorMessage()).isEqualTo("failed");
        assertThat(job.getCompletedAt()).isNotNull();
    }
}
