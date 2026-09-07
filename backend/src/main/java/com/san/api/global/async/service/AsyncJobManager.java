package com.san.api.global.async.service;

import com.san.api.global.async.entity.AsyncJob;
import com.san.api.global.async.entity.JobStatus;
import com.san.api.global.async.entity.JobType;
import com.san.api.global.async.event.JobCreatedEvent;
import com.san.api.global.async.repository.AsyncJobRepository;
import com.san.api.global.audit.context.AuditContextSnapshot;
import com.san.api.global.audit.context.AuditRequestContext;
import com.san.api.global.audit.context.AuditRequestContextHolder;
import com.san.api.global.audit.context.AuditRequesterType;
import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.CommonErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/**
 * 비동기 잡 생성 및 이벤트 발행을 캡슐화하는 서비스.
 *
 * 도메인 서비스에서 enqueue() 한 줄로 비동기 작업을 요청할 수 있도록 한다.
 */
@Service
@RequiredArgsConstructor
public class AsyncJobManager {

    private final AsyncJobRepository asyncJobRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public UUID enqueue(JobType jobType, UUID targetId) {
        return enqueue(jobType, targetId, resolveCurrentUserId());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public UUID enqueue(JobType jobType, UUID targetId, UUID actorUserId) {
        return enqueue(jobType, targetId, actorUserId, requestedByType(actorUserId), null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public UUID enqueue(
            JobType jobType,
            UUID targetId,
            UUID actorUserId,
            AuditRequesterType requestedByType,
            Map<String, Object> requestMetadata
    ) {
        try {
            return saveAndPublish(jobType, targetId, actorUserId, requestedByType, requestMetadata);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(CommonErrorCode.DUPLICATE_RESOURCE, "이미 동일한 작업이 진행 중입니다.");
        }
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public UUID enqueueInCurrentTransaction(JobType jobType, UUID targetId) {
        return enqueueInCurrentTransaction(jobType, targetId, resolveCurrentUserId());
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public UUID enqueueInCurrentTransaction(JobType jobType, UUID targetId, UUID actorUserId) {
        return enqueueInCurrentTransaction(jobType, targetId, actorUserId, requestedByType(actorUserId), null);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public UUID enqueueInCurrentTransaction(
            JobType jobType,
            UUID targetId,
            UUID actorUserId,
            AuditRequesterType requestedByType,
            Map<String, Object> requestMetadata
    ) {
        try {
            return saveAndPublish(jobType, targetId, actorUserId, requestedByType, requestMetadata);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(CommonErrorCode.DUPLICATE_RESOURCE, "이미 동일한 작업이 진행 중입니다.");
        }
    }

    @Transactional(readOnly = true)
    public AsyncJob getJob(UUID jobId) {
        return findJob(jobId);
    }

    @Transactional
    public void markProcessing(UUID jobId) {
        findJob(jobId).updateStatus(JobStatus.PROCESSING);
    }

    @Transactional
    public void markCompleted(UUID jobId) {
        findJob(jobId).updateStatus(JobStatus.COMPLETED);
    }

    @Transactional
    public void markFailed(UUID jobId, String errorMessage) {
        findJob(jobId).fail(errorMessage);
    }

    private AsyncJob findJob(UUID jobId) {
        return asyncJobRepository.findById(jobId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND, "작업을 찾을 수 없습니다."));
    }

    private UUID saveAndPublish(
            JobType jobType,
            UUID targetId,
            UUID actorUserId,
            AuditRequesterType requestedByType,
            Map<String, Object> requestMetadata
    ) {
        AuditRequestContext context = AuditRequestContextHolder.get().orElse(null);
        AuditContextSnapshot auditContext = AuditContextSnapshot.from(
                actorUserId,
                requestedByType,
                context,
                requestMetadata
        );
        AsyncJob job = asyncJobRepository.saveAndFlush(
                AsyncJob.builder()
                        .jobType(jobType)
                        .targetId(targetId)
                        .auditContext(auditContext)
                        .build()
        );
        eventPublisher.publishEvent(new JobCreatedEvent(job.getJobId(), jobType, targetId));
        return job.getJobId();
    }

    private UUID resolveCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof UUID userId) {
            return userId;
        }
        if (principal instanceof String userId && !userId.isBlank()) {
            try {
                return UUID.fromString(userId);
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }
        return null;
    }

    private AuditRequesterType requestedByType(UUID actorUserId) {
        return actorUserId == null ? AuditRequesterType.SYSTEM : AuditRequesterType.USER;
    }
}
