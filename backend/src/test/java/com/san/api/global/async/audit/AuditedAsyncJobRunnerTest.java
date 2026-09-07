package com.san.api.global.async.audit;

import com.san.api.global.async.entity.AsyncJob;
import com.san.api.global.async.entity.JobStatus;
import com.san.api.global.async.entity.JobType;
import com.san.api.global.async.service.AsyncJobManager;
import com.san.api.global.audit.context.AuditContextSnapshot;
import com.san.api.global.audit.context.AuditRequestContext;
import com.san.api.global.audit.context.AuditRequestContextHolder;
import com.san.api.global.audit.context.AuditRequesterType;
import com.san.api.global.audit.dto.AuditRecordCommand;
import com.san.api.global.audit.entity.AuditEventType;
import com.san.api.global.audit.service.AuditRecorder;
import com.san.api.global.audit.support.AuditFailureResolver;
import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.CommonErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuditedAsyncJobRunnerTest {

    private final AsyncJobManager asyncJobManager = mock(AsyncJobManager.class);
    private final AuditRecorder auditRecorder = mock(AuditRecorder.class);
    private final AuditedAsyncJobRunner runner = new AuditedAsyncJobRunner(
            asyncJobManager,
            new AsyncJobAuditSpecRegistry(),
            auditRecorder,
            new AuditFailureResolver()
    );

    @AfterEach
    void tearDown() {
        AuditRequestContextHolder.clear();
    }

    @Test
    void 작업이_성공하면_처리중과_성공_감사_로그를_기록한다() {
        UUID jobId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        AsyncJob processingJob = job(jobId, targetId, JobType.SCRAP_REFINE, JobStatus.PROCESSING);
        AsyncJob completedJob = job(jobId, targetId, JobType.SCRAP_REFINE, JobStatus.COMPLETED);
        AtomicBoolean executed = new AtomicBoolean(false);
        when(asyncJobManager.getJob(jobId)).thenReturn(processingJob, completedJob);

        runner.run(jobId, targetId, JobType.SCRAP_REFINE, () -> executed.set(true));

        ArgumentCaptor<AuditRecordCommand> captor = ArgumentCaptor.forClass(AuditRecordCommand.class);
        verify(asyncJobManager).markProcessing(jobId);
        verify(asyncJobManager).markCompleted(jobId);
        verify(asyncJobManager, never()).markFailed(eq(jobId), org.mockito.ArgumentMatchers.anyString());
        verify(auditRecorder, times(2)).recordSuccessSafely(captor.capture());
        assertThat(executed).isTrue();
        assertThat(captor.getAllValues())
                .extracting(AuditRecordCommand::eventType)
                .containsExactly(AuditEventType.ASYNC_JOB_PROCESSING, AuditEventType.ASYNC_JOB_SUCCEEDED);
        assertThat(captor.getAllValues().get(0).metadata())
                .containsEntry("jobId", jobId)
                .containsEntry("targetId", targetId)
                .containsEntry("requestedByType", "USER");
    }

    @Test
    void 작업이_실패하면_실패_상태와_실패_감사_로그를_기록한다() {
        UUID jobId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        AsyncJob processingJob = job(jobId, targetId, JobType.CARD_ANALYSIS, JobStatus.PROCESSING);
        AsyncJob failedJob = job(jobId, targetId, JobType.CARD_ANALYSIS, JobStatus.FAILED);
        when(asyncJobManager.getJob(jobId)).thenReturn(processingJob, failedJob);

        runner.run(jobId, targetId, JobType.CARD_ANALYSIS, () -> {
            throw new IllegalStateException("실패");
        });

        ArgumentCaptor<AuditRecordCommand> failureCaptor = ArgumentCaptor.forClass(AuditRecordCommand.class);
        verify(asyncJobManager).markProcessing(jobId);
        verify(asyncJobManager, never()).markCompleted(jobId);
        verify(asyncJobManager).markFailed(eq(jobId), contains("IllegalStateException: 실패"));
        verify(auditRecorder).recordFailureSafely(
                failureCaptor.capture(),
                eq("ASYNC_JOB.UNKNOWN_FAILURE"),
                contains("IllegalStateException: 실패")
        );
        assertThat(failureCaptor.getValue().eventType()).isEqualTo(AuditEventType.ASYNC_JOB_FAILED);
        assertThat(failureCaptor.getValue().metadata()).containsEntry("exceptionType", "IllegalStateException");
    }

    @Test
    void cause_chain에_BusinessException이_있으면_실패코드와_메타데이터에_반영한다() {
        UUID jobId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        AsyncJob processingJob = job(jobId, targetId, JobType.CARD_ANALYSIS, JobStatus.PROCESSING);
        AsyncJob failedJob = job(jobId, targetId, JobType.CARD_ANALYSIS, JobStatus.FAILED);
        when(asyncJobManager.getJob(jobId)).thenReturn(processingJob, failedJob);

        runner.run(jobId, targetId, JobType.CARD_ANALYSIS, () -> {
            throw new IllegalStateException("외부 래퍼", new BusinessException(CommonErrorCode.UNAUTHORIZED));
        });

        ArgumentCaptor<AuditRecordCommand> failureCaptor = ArgumentCaptor.forClass(AuditRecordCommand.class);
        verify(asyncJobManager).markFailed(eq(jobId), contains("C003: 인증에 실패했습니다."));
        verify(auditRecorder).recordFailureSafely(
                failureCaptor.capture(),
                eq("C003"),
                contains("C003: 인증에 실패했습니다.")
        );
        assertThat(failureCaptor.getValue().metadata())
                .containsEntry("exceptionType", "IllegalStateException")
                .containsEntry("clientErrorCode", "C003")
                .containsEntry("httpStatus", 401);
    }

    @Test
    void 작업_실행_중에는_저장된_감사_요청_컨텍스트를_복원한다() {
        UUID jobId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        AsyncJob processingJob = job(jobId, targetId, JobType.SCRAP_REFINE, JobStatus.PROCESSING);
        AsyncJob completedJob = job(jobId, targetId, JobType.SCRAP_REFINE, JobStatus.COMPLETED);
        AtomicReference<AuditRequestContext> contextRef = new AtomicReference<>();
        when(asyncJobManager.getJob(jobId)).thenReturn(processingJob, completedJob);

        runner.run(jobId, targetId, JobType.SCRAP_REFINE, () ->
                contextRef.set(AuditRequestContextHolder.get().orElseThrow())
        );

        assertThat(contextRef.get().traceId()).isEqualTo("trace-1");
        assertThat(contextRef.get().ipAddress()).isEqualTo("203.0.113.10");
        assertThat(contextRef.get().userAgent()).isEqualTo("JUnit");
        assertThat(AuditRequestContextHolder.get()).isEmpty();
    }

    @Test
    void 작업이_실패해도_기존_감사_요청_컨텍스트를_복원한다() {
        UUID jobId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        AsyncJob processingJob = job(jobId, targetId, JobType.CARD_ANALYSIS, JobStatus.PROCESSING);
        AsyncJob failedJob = job(jobId, targetId, JobType.CARD_ANALYSIS, JobStatus.FAILED);
        AuditRequestContext previousContext = new AuditRequestContext("previous-trace", "198.51.100.20", "Previous");
        AtomicReference<AuditRequestContext> contextRef = new AtomicReference<>();
        AuditRequestContextHolder.set(previousContext);
        when(asyncJobManager.getJob(jobId)).thenReturn(processingJob, failedJob);

        runner.run(jobId, targetId, JobType.CARD_ANALYSIS, () -> {
            contextRef.set(AuditRequestContextHolder.get().orElseThrow());
            throw new IllegalStateException("실패");
        });

        assertThat(contextRef.get().traceId()).isEqualTo("trace-1");
        assertThat(AuditRequestContextHolder.get()).contains(previousContext);
    }

    private AsyncJob job(UUID jobId, UUID targetId, JobType jobType, JobStatus status) {
        AsyncJob job = AsyncJob.builder()
                .jobType(jobType)
                .targetId(targetId)
                .auditContext(AuditContextSnapshot.builder()
                        .actorUserId(UUID.randomUUID())
                        .traceId("trace-1")
                        .ipAddress("203.0.113.10")
                        .userAgent("JUnit")
                        .requestedByType(AuditRequesterType.USER)
                        .requestMetadata(Map.of("source", "test"))
                        .build())
                .build();
        ReflectionTestUtils.setField(job, "jobId", jobId);
        if (status == JobStatus.PROCESSING) {
            job.updateStatus(JobStatus.PROCESSING);
        }
        if (status == JobStatus.COMPLETED) {
            job.updateStatus(JobStatus.PROCESSING);
            job.updateStatus(JobStatus.COMPLETED);
        }
        if (status == JobStatus.FAILED) {
            job.fail("실패");
        }
        return job;
    }
}
