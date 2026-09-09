package com.san.api.global.async.service;

import com.san.api.global.async.entity.AsyncJob;
import com.san.api.global.async.entity.JobType;
import com.san.api.global.async.repository.AsyncJobRepository;
import com.san.api.global.audit.context.AuditContextSnapshot;
import com.san.api.global.audit.context.AuditRequestContext;
import com.san.api.global.audit.context.AuditRequestContextHolder;
import com.san.api.global.audit.context.AuditRequesterType;
import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.CommonErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AsyncJobManagerTest {

    private final AsyncJobRepository asyncJobRepository = mock(AsyncJobRepository.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final AsyncJobManager asyncJobManager = new AsyncJobManager(asyncJobRepository, eventPublisher);

    @AfterEach
    void tearDown() {
        AuditRequestContextHolder.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void enqueueCapturesCurrentAuditAndSecurityContext() {
        UUID actorUserId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        AuditRequestContextHolder.set(new AuditRequestContext("trace-1", "203.0.113.10", "JUnit"));
        SecurityContextHolder.getContext()
                .setAuthentication(new TestingAuthenticationToken(actorUserId.toString(), null));
        when(asyncJobRepository.saveAndFlush(any(AsyncJob.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        asyncJobManager.enqueue(JobType.SCRAP_REFINE, targetId);

        ArgumentCaptor<AsyncJob> captor = ArgumentCaptor.forClass(AsyncJob.class);
        org.mockito.Mockito.verify(asyncJobRepository).saveAndFlush(captor.capture());
        AsyncJob savedJob = captor.getValue();
        assertThat(savedJob.getJobType()).isEqualTo(JobType.SCRAP_REFINE);
        assertThat(savedJob.getTargetId()).isEqualTo(targetId);
        assertThat(savedJob.getAuditContext().getActorUserId()).isEqualTo(actorUserId);
        assertThat(savedJob.getAuditContext().getTraceId()).isEqualTo("trace-1");
        assertThat(savedJob.getAuditContext().getIpAddress()).isEqualTo("203.0.113.10");
        assertThat(savedJob.getAuditContext().getUserAgent()).isEqualTo("JUnit");
        assertThat(savedJob.getAuditContext().getRequestedByType()).isEqualTo(AuditRequesterType.USER);
    }

    @Test
    void enqueueSupportsExplicitSystemMetadata() {
        UUID targetId = UUID.randomUUID();
        when(asyncJobRepository.saveAndFlush(any(AsyncJob.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        asyncJobManager.enqueue(
                JobType.CARD_ANALYSIS,
                targetId,
                null,
                AuditRequesterType.SCHEDULER,
                Map.of("trigger", "orphan-recovery")
        );

        ArgumentCaptor<AsyncJob> captor = ArgumentCaptor.forClass(AsyncJob.class);
        org.mockito.Mockito.verify(asyncJobRepository).saveAndFlush(captor.capture());
        AsyncJob savedJob = captor.getValue();
        assertThat(savedJob.getAuditContext().getActorUserId()).isNull();
        assertThat(savedJob.getAuditContext().getRequestedByType()).isEqualTo(AuditRequesterType.SCHEDULER);
        assertThat(savedJob.getAuditContext().getRequestMetadata()).containsEntry("trigger", "orphan-recovery");
    }

    @Test
    void getJobForUser_rejectsAnotherUsersJob() {
        UUID ownerId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        AsyncJob job = AsyncJob.builder()
                .jobType(JobType.TIL_GENERATION)
                .targetId(UUID.randomUUID())
                .auditContext(AuditContextSnapshot.builder().actorUserId(ownerId).build())
                .build();
        org.springframework.test.util.ReflectionTestUtils.setField(job, "jobId", jobId);
        when(asyncJobRepository.findById(jobId)).thenReturn(Optional.of(job));

        assertThatThrownBy(() -> asyncJobManager.getJobForUser(jobId, otherUserId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", CommonErrorCode.UNAUTHORIZED);
    }
}
