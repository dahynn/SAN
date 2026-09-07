package com.san.api.global.scheduler.service;

import com.san.api.domain.scrap.entity.Scrap;
import com.san.api.domain.scrap.repository.ScrapRepository;
import com.san.api.global.async.entity.JobStatus;
import com.san.api.global.async.entity.JobType;
import com.san.api.global.async.service.AsyncJobManager;
import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.CommonErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/** 지식카드가 생성되지 않은 고아 스크랩을 찾아 CARD_ANALYSIS 잡을 재등록하는 서비스 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrphanScrapRecoveryService {

    private static final List<JobStatus> ACTIVE_STATUSES = List.of(JobStatus.PENDING, JobStatus.PROCESSING);

    private final ScrapRepository scrapRepository;
    private final AsyncJobManager asyncJobManager;

    /**
     * 지식카드가 생성되지 않은 고아 스크랩을 탐지해 CARD_ANALYSIS 잡을 재등록합니다.
     *
     * <p>이미 PENDING/PROCESSING 잡이 존재하는 스크랩은 조회 단계에서 제외되므로
     * Ghost Job 복구 대상과 중복 처리되지 않습니다.</p>
     */
    public void recover() {
        List<Scrap> orphans = scrapRepository.findOrphanScraps(JobType.CARD_ANALYSIS, ACTIVE_STATUSES);

        int enqueued = 0;
        for (Scrap scrap : orphans) {
            try {
                asyncJobManager.enqueue(JobType.CARD_ANALYSIS, scrap.getScrapId());
                enqueued++;
            } catch (BusinessException e) {
                if (e.getErrorCode() != CommonErrorCode.DUPLICATE_RESOURCE) {
                    log.warn("[OrphanScrapRecovery] enqueue failed scrapId={}: {}", scrap.getScrapId(), e.getMessage());
                }
            }
        }

        log.info("[OrphanScrapRecovery] orphans={}, enqueued={}", orphans.size(), enqueued);
    }
}
