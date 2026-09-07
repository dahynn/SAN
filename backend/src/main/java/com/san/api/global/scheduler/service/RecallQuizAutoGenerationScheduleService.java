package com.san.api.global.scheduler.service;

import com.san.api.domain.knowledge.repository.KnowledgeCardRepository;
import com.san.api.domain.recall.entity.RecallQuizGeneration;
import com.san.api.domain.recall.entity.RecallQuizType;
import com.san.api.domain.recall.repository.RecallQuizGenerationRepository;
import com.san.api.domain.user.entity.User;
import com.san.api.domain.user.repository.UserRepository;
import com.san.api.global.async.entity.JobType;
import com.san.api.global.async.service.AsyncJobManager;
import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.CommonErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** 전날 스크랩이 있는 사용자에게 리콜 퀴즈 생성 잡을 자동 enqueue하는 서비스 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecallQuizAutoGenerationScheduleService {

    private final KnowledgeCardRepository knowledgeCardRepository;
    private final UserRepository userRepository;
    private final RecallQuizGenerationRepository recallQuizGenerationRepository;
    private final AsyncJobManager asyncJobManager;

    /**
     * 전날 스크랩이 있는 사용자 전원에게 OX·SHORT_ANSWER 퀴즈 생성 잡을 enqueue합니다.
     *
     * <p>이미 해당 날짜·타입의 RecallQuizGeneration이 존재하면 skip합니다.</p>
     */
    public void generate() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        LocalDateTime startAt = yesterday.atStartOfDay();
        LocalDateTime endAt = yesterday.plusDays(1).atStartOfDay();

        List<UUID> userIds = knowledgeCardRepository.findDistinctUserIdsByScrapCreatedBetween(startAt, endAt);

        int enqueued = 0;
        for (UUID userId : userIds) {
            for (RecallQuizType quizType : RecallQuizType.values()) {
                try {
                    enqueued += enqueueIfAbsent(userId, yesterday, quizType);
                } catch (Exception e) {
                    log.warn("[RecallQuizAutoGeneration] failed userId={}, type={}: {}",
                            userId, quizType, e.getMessage());
                }
            }
        }

        log.info("[RecallQuizAutoGeneration] date={}, users={}, enqueued={}", yesterday, userIds.size(), enqueued);
    }

    private int enqueueIfAbsent(UUID userId, LocalDate targetDate, RecallQuizType quizType) {
        boolean exists = recallQuizGenerationRepository
                .findFirstByUser_UserIdAndTargetDateAndQuizTypeOrderByCreatedAtDesc(userId, targetDate, quizType)
                .isPresent();
        if (exists) {
            return 0;
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND));
        RecallQuizGeneration generation = recallQuizGenerationRepository.save(
                RecallQuizGeneration.builder()
                        .user(user)
                        .targetDate(targetDate)
                        .quizType(quizType)
                        .build()
        );
        asyncJobManager.enqueue(JobType.RECALL_QUIZ_GENERATION, generation.getGenerationId());
        return 1;
    }
}
