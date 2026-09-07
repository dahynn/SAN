package com.san.api.global.scheduler.service;

import com.san.api.domain.knowledge.repository.KnowledgeCardRepository;
import com.san.api.domain.til.dto.request.TilGenerateRequest;
import com.san.api.domain.til.service.TilService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** 전날 지식카드가 생성된 사용자 전원에게 TIL을 자동 생성하는 서비스 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TilAutoGenerationScheduleService {

    private final KnowledgeCardRepository knowledgeCardRepository;
    private final TilService tilService;

    /**
     * 전날 지식카드가 생성된 사용자 전원에게 TIL을 자동 생성합니다.
     *
     * <p>전날 스크랩 기준으로 활동한 사용자를 조회하고,
     * 각 사용자에 대해 빈 DailySummary를 생성한 뒤 TIL_GENERATION 잡을 enqueue합니다.
     * 이미 당일 TIL이 있어도 새로 생성하며, 다건 허용 구조를 따릅니다.</p>
     */
    public void generate() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        LocalDateTime startAt = yesterday.atStartOfDay();
        LocalDateTime endAt = yesterday.plusDays(1).atStartOfDay();

        List<UUID> userIds = knowledgeCardRepository.findDistinctUserIdsByScrapCreatedBetween(startAt, endAt);

        int enqueued = 0;
        for (UUID userId : userIds) {
            try {
                tilService.requestGeneration(userId, new TilGenerateRequest(yesterday));
                enqueued++;
            } catch (Exception e) {
                log.warn("[TilAutoGeneration] failed userId={}: {}", userId, e.getMessage());
            }
        }

        log.info("[TilAutoGeneration] date={}, users={}, enqueued={}", yesterday, userIds.size(), enqueued);
    }
}
