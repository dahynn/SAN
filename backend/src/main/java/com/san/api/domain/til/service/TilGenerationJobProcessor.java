package com.san.api.domain.til.service;

import com.san.api.domain.til.entity.DailySummary;
import com.san.api.global.async.audit.AuditedAsyncJobRunner;
import com.san.api.global.async.entity.JobType;
import com.san.api.global.async.event.JobCreatedEvent;
import com.san.api.global.async.processor.AsyncJobProcessor;
import com.san.api.global.external.ai.dto.response.AiTilResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;

/** TIL 생성 비동기 작업 처리기 */
@Component
@RequiredArgsConstructor
public class TilGenerationJobProcessor implements AsyncJobProcessor {

    private final AuditedAsyncJobRunner auditedAsyncJobRunner;
    private final DailySummaryService dailySummaryService;
    private final TilGenerationService tilGenerationService;

    @Override
    public JobType supports() {
        return JobType.TIL_GENERATION;
    }

    /**
     * TIL_GENERATION 작업 생성 이벤트를 수신합니다.
     *
     * @param event 비동기 작업 생성 이벤트
     */
    @Async("aiJobExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(JobCreatedEvent event) {
        handleIfSupported(event);
    }

    /**
     * TIL 생성 작업을 감사 실행기로 위임해 처리합니다.
     *
     * @param jobId 비동기 작업 ID
     * @param targetId 생성 대상 DailySummary ID
     */
    @Override
    public void process(UUID jobId, UUID targetId) {
        auditedAsyncJobRunner.run(jobId, targetId, JobType.TIL_GENERATION, () -> {
            DailySummary summary = dailySummaryService.getSummary(targetId);
            AiTilResponse response = tilGenerationService.generate(
                    summary.getUser().getUserId(),
                    summary.getTargetDate()
            );

            dailySummaryService.updateGeneratedResult(
                    summary.getSummaryId(),
                    response.title(),
                    response.tilMarkdown(),
                    response.embedding()
            );
        });
    }
}
