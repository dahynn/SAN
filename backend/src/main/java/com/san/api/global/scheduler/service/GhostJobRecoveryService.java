package com.san.api.global.scheduler.service;

import com.san.api.global.async.entity.AsyncJob;
import com.san.api.global.async.entity.JobStatus;
import com.san.api.global.async.repository.AsyncJobRepository;
import com.san.api.global.async.service.AsyncJobManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

/** 오랫동안 PENDING/PROCESSING 상태로 멈춘 유령 잡을 감지해 재등록하는 서비스 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GhostJobRecoveryService {

    private static final Duration PENDING_STALE_THRESHOLD = Duration.ofMinutes(30);
    private static final Duration PROCESSING_STALE_THRESHOLD = Duration.ofHours(1);

    private final AsyncJobRepository asyncJobRepository;
    private final AsyncJobManager asyncJobManager;

    /**
     * 스테일 잡을 탐지해 FAILED 처리 후 신규 잡으로 재등록합니다.
     *
     * <p>PENDING > 30분, PROCESSING > 1시간인 잡을 스테일로 판단합니다.
     * 스테일 잡은 FAILED로 닫고 동일 타입·대상으로 새 잡을 enqueue합니다.</p>
     */
    public void recover() {
        LocalDateTime now = LocalDateTime.now();

        List<AsyncJob> stalePending = asyncJobRepository.findByStatusAndCreatedAtBefore(
                JobStatus.PENDING, now.minus(PENDING_STALE_THRESHOLD));

        List<AsyncJob> staleProcessing = asyncJobRepository.findByStatusAndCreatedAtBefore(
                JobStatus.PROCESSING, now.minus(PROCESSING_STALE_THRESHOLD));

        Stream.concat(stalePending.stream(), staleProcessing.stream())
                .forEach(this::requeue);

        log.info("[GhostJobRecovery] pending={}, processing={} stale jobs requeued",
                stalePending.size(), staleProcessing.size());
    }

    private void requeue(AsyncJob job) {
        try {
            asyncJobManager.markFailed(job.getJobId(), "스테일 잡으로 판단되어 배치 스케줄러가 재등록함");
            asyncJobManager.enqueue(job.getJobType(), job.getTargetId());
        } catch (Exception e) {
            log.warn("[GhostJobRecovery] requeue failed jobId={} type={}: {}",
                    job.getJobId(), job.getJobType(), e.getMessage());
        }
    }
}
