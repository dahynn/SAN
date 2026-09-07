package com.san.api.global.scheduler.service;

import com.san.api.global.async.entity.AsyncJob;
import com.san.api.global.async.entity.JobStatus;
import com.san.api.global.async.entity.JobType;
import com.san.api.global.async.repository.AsyncJobRepository;
import com.san.api.global.async.service.AsyncJobManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FailedJobRecoveryServiceTest {

    @Mock
    private AsyncJobRepository asyncJobRepository;

    @Mock
    private AsyncJobManager asyncJobManager;

    @InjectMocks
    private FailedJobRecoveryService failedJobRecoveryService;

    @Test
    void recover_whenFailCountBelowMax_enqueuesNewJob() {
        UUID targetId = UUID.randomUUID();
        AsyncJob failedJob = failedJob(JobType.CARD_ANALYSIS, targetId);
        when(asyncJobRepository.findByStatus(JobStatus.FAILED)).thenReturn(List.of(failedJob));
        when(asyncJobRepository.countByTargetIdAndJobTypeAndStatus(targetId, JobType.CARD_ANALYSIS, JobStatus.FAILED))
                .thenReturn(1L);

        failedJobRecoveryService.recover();

        verify(asyncJobManager).enqueue(JobType.CARD_ANALYSIS, targetId);
    }

    @Test
    void recover_whenFailCountReachesMax_skipsJob() {
        UUID targetId = UUID.randomUUID();
        AsyncJob failedJob = failedJob(JobType.CARD_ANALYSIS, targetId);
        when(asyncJobRepository.findByStatus(JobStatus.FAILED)).thenReturn(List.of(failedJob));
        when(asyncJobRepository.countByTargetIdAndJobTypeAndStatus(targetId, JobType.CARD_ANALYSIS, JobStatus.FAILED))
                .thenReturn(3L);

        failedJobRecoveryService.recover();

        verify(asyncJobManager, never()).enqueue(any(), any());
    }

    @Test
    void recover_whenEnqueueThrows_continuesRemainingJobs() {
        UUID targetId1 = UUID.randomUUID();
        UUID targetId2 = UUID.randomUUID();
        AsyncJob job1 = failedJob(JobType.CARD_ANALYSIS, targetId1);
        AsyncJob job2 = failedJob(JobType.SCRAP_REFINE, targetId2);
        when(asyncJobRepository.findByStatus(JobStatus.FAILED)).thenReturn(List.of(job1, job2));
        when(asyncJobRepository.countByTargetIdAndJobTypeAndStatus(eq(targetId1), eq(JobType.CARD_ANALYSIS), eq(JobStatus.FAILED)))
                .thenReturn(1L);
        when(asyncJobRepository.countByTargetIdAndJobTypeAndStatus(eq(targetId2), eq(JobType.SCRAP_REFINE), eq(JobStatus.FAILED)))
                .thenReturn(1L);
        doThrow(new RuntimeException("enqueue error")).when(asyncJobManager).enqueue(JobType.CARD_ANALYSIS, targetId1);

        failedJobRecoveryService.recover();

        verify(asyncJobManager).enqueue(JobType.SCRAP_REFINE, targetId2);
    }

    private AsyncJob failedJob(JobType jobType, UUID targetId) {
        AsyncJob job = AsyncJob.builder()
                .jobType(jobType)
                .targetId(targetId)
                .build();
        job.fail("error");
        return job;
    }
}
