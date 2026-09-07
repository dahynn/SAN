package com.san.api.global.async.audit;

import com.san.api.global.async.entity.AsyncJob;
import com.san.api.global.async.entity.JobType;
import com.san.api.global.async.service.AsyncJobManager;
import com.san.api.global.audit.context.AuditContextSnapshot;
import com.san.api.global.audit.context.AuditRequestContext;
import com.san.api.global.audit.context.AuditRequestContextHolder;
import com.san.api.global.audit.context.AuditRequesterType;
import com.san.api.global.audit.dto.AuditRecordCommand;
import com.san.api.global.audit.service.AuditRecorder;
import com.san.api.global.audit.support.AuditFailureResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AuditedAsyncJobRunner {

    private static final String UNKNOWN_FAILURE_REASON_CODE = "ASYNC_JOB.UNKNOWN_FAILURE";
    private static final String DEFAULT_FAILURE_MESSAGE = "비동기 작업 처리 중 오류가 발생했습니다.";

    private final AsyncJobManager asyncJobManager;
    private final AsyncJobAuditSpecRegistry auditSpecRegistry;
    private final AuditRecorder auditRecorder;
    private final AuditFailureResolver auditFailureResolver;

    /**
     * 비동기 작업의 상태 전이와 운영 감사 로그 기록을 공통 흐름으로 처리합니다.
     *
     * @param jobId 처리할 비동기 작업 ID
     * @param targetId 비동기 작업이 처리하는 업무 대상 ID
     * @param jobType 비동기 작업 유형
     * @param task 실제 도메인 작업
     */
    public void run(UUID jobId, UUID targetId, JobType jobType, AsyncJobTask task) {
        run(jobId, targetId, jobType, task, DEFAULT_FAILURE_MESSAGE);
    }

    /**
     * 비동기 작업의 상태 전이와 운영 감사 로그 기록을 공통 흐름으로 처리합니다.
     *
     * @param jobId 처리할 비동기 작업 ID
     * @param targetId 비동기 작업이 처리하는 업무 대상 ID
     * @param jobType 비동기 작업 유형
     * @param task 실제 도메인 작업
     * @param failureMessageFallback 예외 메시지가 없을 때 사용할 기본 실패 메시지
     */
    public void run(
            UUID jobId,
            UUID targetId,
            JobType jobType,
            AsyncJobTask task,
            String failureMessageFallback
    ) {
        AsyncJobAuditSpec spec = auditSpecRegistry.get(jobType);

        asyncJobManager.markProcessing(jobId);
        AsyncJob processingJob = asyncJobManager.getJob(jobId);
        recordProcessing(processingJob, targetId, spec);

        try {
            runWithAuditContext(processingJob, task);
            asyncJobManager.markCompleted(jobId);
            recordSucceeded(asyncJobManager.getJob(jobId), targetId, spec);
        } catch (Exception e) {
            String failureMessage = auditFailureResolver.failureMessage(e, failureMessageFallback);
            asyncJobManager.markFailed(jobId, failureMessage);
            recordFailed(asyncJobManager.getJob(jobId), targetId, spec, e, failureMessage);
        }
    }

    private void runWithAuditContext(AsyncJob job, AsyncJobTask task) throws Exception {
        AuditRequestContext previousContext = AuditRequestContextHolder.get().orElse(null);
        applyAuditContext(auditContext(job));
        try {
            task.run();
        } finally {
            restoreAuditContext(previousContext);
        }
    }

    private void applyAuditContext(AuditContextSnapshot auditContext) {
        auditContext.toRequestContext()
                .ifPresentOrElse(AuditRequestContextHolder::set, AuditRequestContextHolder::clear);
    }

    private void restoreAuditContext(AuditRequestContext previousContext) {
        if (previousContext == null) {
            AuditRequestContextHolder.clear();
            return;
        }
        AuditRequestContextHolder.set(previousContext);
    }

    private void recordProcessing(AsyncJob job, UUID targetId, AsyncJobAuditSpec spec) {
        auditRecorder.recordSuccessSafely(command(job, targetId, spec, spec.processingEventType(), null));
    }

    private void recordSucceeded(AsyncJob job, UUID targetId, AsyncJobAuditSpec spec) {
        auditRecorder.recordSuccessSafely(command(job, targetId, spec, spec.succeededEventType(), null));
    }

    private void recordFailed(
            AsyncJob job,
            UUID targetId,
            AsyncJobAuditSpec spec,
            Exception exception,
            String failureMessage
    ) {
        auditRecorder.recordFailureSafely(
                command(job, targetId, spec, spec.failedEventType(), failureMetadata(exception)),
                failureReasonCode(exception),
                failureMessage
        );
    }

    private AuditRecordCommand command(
            AsyncJob job,
            UUID targetId,
            AsyncJobAuditSpec spec,
            com.san.api.global.audit.entity.AuditEventType eventType,
            Map<String, Object> additionalMetadata
    ) {
        AuditContextSnapshot auditContext = auditContext(job);
        return AuditRecordCommand.builder()
                .actorUserId(auditContext.getActorUserId())
                .traceId(auditContext.getTraceId())
                .eventDomain(spec.eventDomain())
                .eventType(eventType)
                .targetType(spec.targetType().code())
                .targetId(job.getJobId())
                .ipAddress(auditContext.getIpAddress())
                .userAgent(auditContext.getUserAgent())
                .metadata(metadata(job, targetId, auditContext, additionalMetadata))
                .build();
    }

    private AuditContextSnapshot auditContext(AsyncJob job) {
        return job.getAuditContext() == null ? AuditContextSnapshot.empty() : job.getAuditContext();
    }

    private Map<String, Object> metadata(
            AsyncJob job,
            UUID targetId,
            AuditContextSnapshot auditContext,
            Map<String, Object> additionalMetadata
    ) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("jobId", job.getJobId());
        metadata.put("jobType", job.getJobType().name());
        metadata.put("targetId", targetId);
        metadata.put("status", job.getStatus().name());
        metadata.put("requestedByType", requestedByType(auditContext).name());
        if (job.getStartedAt() != null) {
            metadata.put("startedAt", job.getStartedAt().toString());
        }
        if (job.getCompletedAt() != null) {
            metadata.put("completedAt", job.getCompletedAt().toString());
        }
        if (auditContext.getRequestMetadata() != null && !auditContext.getRequestMetadata().isEmpty()) {
            metadata.put("requestMetadata", auditContext.getRequestMetadata());
        }
        if (additionalMetadata != null) {
            metadata.putAll(additionalMetadata);
        }
        return metadata;
    }

    private Map<String, Object> failureMetadata(Exception exception) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("exceptionType", exception.getClass().getSimpleName());
        metadata.putAll(auditFailureResolver.failureMetadata(exception));
        return metadata;
    }

    private AuditRequesterType requestedByType(AuditContextSnapshot auditContext) {
        return auditContext.getRequestedByType() == null ? AuditRequesterType.SYSTEM : auditContext.getRequestedByType();
    }

    private String failureReasonCode(Exception exception) {
        return auditFailureResolver.failureReasonCode(exception, UNKNOWN_FAILURE_REASON_CODE);
    }
}
