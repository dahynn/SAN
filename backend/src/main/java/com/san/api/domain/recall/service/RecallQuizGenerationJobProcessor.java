package com.san.api.domain.recall.service;

import com.san.api.domain.recall.dto.request.RecallQuizGenerateRequest;
import com.san.api.domain.recall.entity.RecallQuizGeneration;
import com.san.api.domain.recall.repository.RecallQuizGenerationRepository;
import com.san.api.global.async.audit.AuditedAsyncJobRunner;
import com.san.api.global.async.entity.JobType;
import com.san.api.global.async.event.JobCreatedEvent;
import com.san.api.global.async.processor.AsyncJobProcessor;
import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.CommonErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;

/** 리콜 퀴즈 생성 비동기 작업 처리기 */
@Component
@RequiredArgsConstructor
public class RecallQuizGenerationJobProcessor implements AsyncJobProcessor {

    private final AuditedAsyncJobRunner auditedAsyncJobRunner;
    private final RecallQuizGenerationRepository recallQuizGenerationRepository;
    private final RecallQuizGenerationService recallQuizGenerationService;

    @Override
    public JobType supports() {
        return JobType.RECALL_QUIZ_GENERATION;
    }

    /**
     * RECALL_QUIZ_GENERATION 작업 생성 이벤트를 수신합니다.
     *
     * @param event 비동기 작업 생성 이벤트
     */
    @Async("aiJobExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(JobCreatedEvent event) {
        handleIfSupported(event);
    }

    /**
     * 리콜 퀴즈 생성 작업을 감사 실행기로 위임해 처리합니다.
     *
     * @param jobId 비동기 작업 ID
     * @param targetId 리콜 퀴즈 생성 ID
     */
    @Override
    public void process(UUID jobId, UUID targetId) {
        auditedAsyncJobRunner.run(jobId, targetId, JobType.RECALL_QUIZ_GENERATION, () -> {
            RecallQuizGeneration generation = recallQuizGenerationRepository.findByGenerationIdWithUser(targetId)
                    .orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND));
            RecallQuizGenerateRequest request = new RecallQuizGenerateRequest(
                    generation.getTargetDate(),
                    generation.getQuizType()
            );

            recallQuizGenerationService.generate(generation.getUser().getUserId(), request);
        });
    }
}
