package com.san.api.domain.recall.repository;

import com.san.api.domain.recall.entity.RecallQuiz;
import com.san.api.domain.recall.entity.RecallQuizType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** 리콜 퀴즈 Repository */
public interface RecallQuizRepository extends JpaRepository<RecallQuiz, UUID> {

    /** 중복 생성을 막기 위한 기존 퀴즈 조회 */
    List<RecallQuiz> findAllByUser_UserIdAndDailySummary_SummaryIdAndQuizTypeOrderByCreatedAtAsc(
            UUID userId,
            UUID summaryId,
            RecallQuizType quizType
    );

    /** 날짜와 유형 기준 리콜 퀴즈 조회 */
    List<RecallQuiz> findAllByUser_UserIdAndDailySummary_TargetDateAndQuizTypeOrderByCreatedAtAsc(
            UUID userId,
            LocalDate targetDate,
            RecallQuizType quizType
    );

    /** 사용자 소유 리콜 퀴즈 조회 */
    Optional<RecallQuiz> findByQuizIdAndUser_UserId(UUID quizId, UUID userId);
}
