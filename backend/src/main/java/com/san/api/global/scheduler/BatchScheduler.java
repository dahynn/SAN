package com.san.api.global.scheduler;

import com.san.api.global.outbox.service.OutboxEventRelayService;
import com.san.api.global.scheduler.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 배치 스케줄러 진입점. 비즈니스 로직 없이 각 서비스의 배치 메서드를 호출합니다. */
@Component
@RequiredArgsConstructor
public class BatchScheduler {

    private final GhostJobRecoveryService ghostJobRecoveryService;
    private final OrphanScrapRecoveryService orphanScrapRecoveryService;
    private final OrphanScrapRefineRecoveryService orphanScrapRefineRecoveryService;
    private final FailedJobRecoveryService failedJobRecoveryService;
    private final TilAutoGenerationScheduleService tilAutoGenerationScheduleService;
    private final RecallQuizAutoGenerationScheduleService recallQuizAutoGenerationScheduleService;
    private final OutboxEventRelayService outboxEventRelayService;

    /**
     * 유령 잡, 고아 스크랩, 고아 SCRAP_REFINE 복구 배치. 2시간마다 실행.
     */
    // @Scheduled(cron = "0 0 */2 * * *")
    public void runRecovery() {
        ghostJobRecoveryService.recover();
        orphanScrapRecoveryService.recover();
        orphanScrapRefineRecoveryService.recover();
        failedJobRecoveryService.recover();
    }

    /** 처리 가능한 Outbox 이벤트를 30초마다 외부 시스템으로 전달합니다. */
    @Scheduled(fixedDelayString = "${outbox.relay.fixed-delay-ms:60000}")
    public void relayOutboxEvents() {
        outboxEventRelayService.relayDueEvents();
    }

    /** TIL 자동 생성 배치를 매일 03:00에 실행합니다. */
    // @Scheduled(cron = "0 0 3 * * *")
    public void runTilAutoGeneration() {
        tilAutoGenerationScheduleService.generate();
        recallQuizAutoGenerationScheduleService.generate();
    }
}
