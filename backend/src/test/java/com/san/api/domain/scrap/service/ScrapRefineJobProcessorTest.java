package com.san.api.domain.scrap.service;

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

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ScrapRefineJobProcessorTest {

    @Mock
    private AuditedAsyncJobRunner auditedAsyncJobRunner;

    @Mock
    private ScrapRefineService scrapRefineService;

    @InjectMocks
    private ScrapRefineJobProcessor scrapRefineJobProcessor;

    @Test
    void process_감사_실행기에_정제_작업을_위임한다() throws Exception {
        UUID jobId = UUID.randomUUID();
        UUID scrapId = UUID.randomUUID();

        scrapRefineJobProcessor.process(jobId, scrapId);

        ArgumentCaptor<AsyncJobTask> taskCaptor = ArgumentCaptor.forClass(AsyncJobTask.class);
        verify(auditedAsyncJobRunner).run(
                org.mockito.ArgumentMatchers.eq(jobId),
                org.mockito.ArgumentMatchers.eq(scrapId),
                org.mockito.ArgumentMatchers.eq(JobType.SCRAP_REFINE),
                taskCaptor.capture()
        );

        taskCaptor.getValue().run();

        verify(scrapRefineService).refine(scrapId);
    }
}
