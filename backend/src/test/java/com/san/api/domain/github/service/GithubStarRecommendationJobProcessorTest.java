package com.san.api.domain.github.service;

import com.san.api.global.async.audit.AsyncJobTask;
import com.san.api.global.async.audit.AuditedAsyncJobRunner;
import com.san.api.global.async.entity.JobType;
import com.san.api.global.async.event.JobCreatedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GithubStarRecommendationJobProcessorTest {

    private AuditedAsyncJobRunner auditedAsyncJobRunner;
    private GithubStarRecommendationAnalysisService githubStarRecommendationAnalysisService;
    private GithubStarRecommendationJobProcessor processor;

    @BeforeEach
    void setUp() {
        auditedAsyncJobRunner = mock(AuditedAsyncJobRunner.class);
        githubStarRecommendationAnalysisService = mock(GithubStarRecommendationAnalysisService.class);
        processor = new GithubStarRecommendationJobProcessor(
                auditedAsyncJobRunner,
                githubStarRecommendationAnalysisService
        );
    }

    @Test
    void handle_processesGithubStarRecommendationJobOnly() throws Exception {
        UUID jobId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        JobCreatedEvent event = mock(JobCreatedEvent.class);

        when(event.getJobType()).thenReturn(JobType.GITHUB_STAR_RECOMMENDATION);
        when(event.getJobId()).thenReturn(jobId);
        when(event.getTargetId()).thenReturn(targetId);

        processor.handle(event);

        ArgumentCaptor<AsyncJobTask> taskCaptor = ArgumentCaptor.forClass(AsyncJobTask.class);
        verify(auditedAsyncJobRunner).run(
                eq(jobId),
                eq(targetId),
                eq(JobType.GITHUB_STAR_RECOMMENDATION),
                taskCaptor.capture()
        );

        taskCaptor.getValue().run();

        verify(githubStarRecommendationAnalysisService).analyzeAndSave(targetId, jobId);
    }

    @Test
    void handle_ignoresOtherJobType() {
        UUID jobId = UUID.randomUUID();
        JobCreatedEvent event = mock(JobCreatedEvent.class);

        when(event.getJobType()).thenReturn(JobType.CARD_ANALYSIS);
        when(event.getJobId()).thenReturn(jobId);

        processor.handle(event);

        verify(auditedAsyncJobRunner, never()).run(
                eq(jobId),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
        verify(githubStarRecommendationAnalysisService, never()).analyzeAndSave(null, jobId);
    }
}
