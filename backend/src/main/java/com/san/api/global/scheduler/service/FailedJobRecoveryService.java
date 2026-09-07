package com.san.api.global.scheduler.service;

import com.san.api.global.async.entity.AsyncJob;
import com.san.api.global.async.entity.JobStatus;
import com.san.api.global.async.repository.AsyncJobRepository;
import com.san.api.global.async.service.AsyncJobManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/** FAILED 상태로 굳은 잡을 배치로 재시도하는 서비스 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FailedJobRecoveryService {

    private static final int MAX_RETRY_COUNT = 3;

    private final AsyncJobRepository asyncJobRepository;
    private final AsyncJobManager asyncJobManager;

    /**
     * FAILED 잡을 조회해 재시도 횟수가 MAX_RETRY_COUNT 미만이면 동일 (jobType, targetId)로 새 잡을 enqueue합니다.
     *
     * <p>기존 FAILED 잡은 그대로 두고 새 잡을 추가합니다.
     * 동일 (targetId, jobType)의 FAILED 잡 수가 MAX_RETRY_COUNT 이상이면 skip합니다.</p>
     */
    public void recover() {
        List<AsyncJob> failedJobs = asyncJobRepository.findByStatus(JobStatus.FAILED);

        int requeued = 0;
        for (AsyncJob job : failedJobs) {
            try {
                long failCount = asyncJobRepository.countByTargetIdAndJobTypeAndStatus(
                        job.getTargetId(), job.getJobType(), JobStatus.FAILED);
                if (failCount >= MAX_RETRY_COUNT) {
                    continue;
                }
                log.info("[FailedJobRecovery] requeue jobId={}, type={}, failCount={}",
                        job.getJobId(), job.getJobType(), failCount);
                asyncJobManager.enqueue(job.getJobType(), job.getTargetId());
                requeued++;
            } catch (Exception e) {
                log.warn("[FailedJobRecovery] requeue failed jobId={}: {}", job.getJobId(), e.getMessage());
            }
        }

        log.info("[FailedJobRecovery] requeued={} jobs", requeued);
    }
}
