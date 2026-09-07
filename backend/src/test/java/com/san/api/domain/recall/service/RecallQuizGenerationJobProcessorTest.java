package com.san.api.domain.recall.service;

import com.san.api.domain.recall.repository.RecallQuizGenerationRepository;
import com.san.api.global.async.audit.AsyncJobTask;
import com.san.api.global.async.audit.AuditedAsyncJobRunner;
import com.san.api.global.async.entity.JobType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RecallQuizGenerationJobProcessorTest {

    @Mock
    private AuditedAsyncJobRunner auditedAsyncJobRunner;

    @Mock
    private RecallQuizGenerationRepository recallQuizGenerationRepository;

    @Mock
    private RecallQuizGenerationService recallQuizGenerationService;

    @InjectMocks
    private RecallQuizGenerationJobProcessor recallQuizGenerationJobProcessor;

    @Test
    void process_감사_실행기에_퀴즈_생성_작업을_위임한다() {
        UUID jobId = UUID.randomUUID();
        UUID generationId = UUID.randomUUID();

        recallQuizGenerationJobProcessor.process(jobId, generationId);

        ArgumentCaptor<AsyncJobTask> taskCaptor = ArgumentCaptor.forClass(AsyncJobTask.class);
        verify(auditedAsyncJobRunner).run(
                eq(jobId),
                eq(generationId),
                eq(JobType.RECALL_QUIZ_GENERATION),
                taskCaptor.capture()
        );
    }
}
