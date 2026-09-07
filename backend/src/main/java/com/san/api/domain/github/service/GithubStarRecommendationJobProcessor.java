package com.san.api.domain.github.service;

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

/** GitHub Star 추천 비동기 작업 처리기 */
@Component
@RequiredArgsConstructor
public class GithubStarRecommendationJobProcessor implements AsyncJobProcessor {

    private final AuditedAsyncJobRunner auditedAsyncJobRunner;
    private final GithubStarRecommendationAnalysisService githubStarRecommendationAnalysisService;

    @Override
    public JobType supports() {
        return JobType.GITHUB_STAR_RECOMMENDATION;
    }

    /**
     * GITHUB_STAR_RECOMMENDATION 작업 생성 이벤트를 수신합니다.
     *
     * @param event 비동기 작업 생성 이벤트
     */
    @Async("aiJobExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(JobCreatedEvent event) {
        handleIfSupported(event);
    }

    /**
     * GitHub Star 추천 URL 분석 작업을 감사 실행기로 위임해 처리합니다.
     *
     * @param jobId 비동기 작업 ID
     * @param targetId 추천을 요청한 사용자 ID
     */
    @Override
    public void process(UUID jobId, UUID targetId) {
        auditedAsyncJobRunner.run(jobId, targetId, JobType.GITHUB_STAR_RECOMMENDATION,
                () -> githubStarRecommendationAnalysisService.analyzeAndSave(targetId, jobId));
    }
}
