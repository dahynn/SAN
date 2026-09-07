package com.san.api.domain.scrap.service;

import com.san.api.global.async.audit.AuditedAsyncJobRunner;
import com.san.api.global.async.entity.JobType;
import com.san.api.global.async.event.JobCreatedEvent;
import com.san.api.global.async.processor.AsyncJobProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;

/** 수집 원본 정제 비동기 작업 처리기 */
@Component
@RequiredArgsConstructor
public class ScrapRefineJobProcessor implements AsyncJobProcessor {

    private final AuditedAsyncJobRunner auditedAsyncJobRunner;
    private final ScrapRefineService scrapRefineService;

    @Override
    public JobType supports() {
        return JobType.SCRAP_REFINE;
    }

    /**
     * SCRAP_REFINE 작업 생성 이벤트를 수신합니다.
     *
     * @param event 비동기 작업 생성 이벤트
     */
    @Async("aiJobExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(JobCreatedEvent event) {
        handleIfSupported(event);
    }

    /**
     * 수집 원본 정제 작업을 감사 실행기로 위임해 처리합니다.
     *
     * @param jobId 비동기 작업 ID
     * @param targetId 정제 대상 Scrap ID
     */
    @Override
    public void process(UUID jobId, UUID targetId) {
        auditedAsyncJobRunner.run(jobId, targetId, JobType.SCRAP_REFINE,
                () -> scrapRefineService.refine(targetId));
    }
}
