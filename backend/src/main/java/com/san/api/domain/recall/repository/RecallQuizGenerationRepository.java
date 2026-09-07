package com.san.api.domain.recall.repository;

import com.san.api.domain.recall.entity.RecallQuizGeneration;
import com.san.api.domain.recall.entity.RecallQuizType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/** 리콜 퀴즈 생성 Repository */
public interface RecallQuizGenerationRepository extends JpaRepository<RecallQuizGeneration, UUID> {

    /** 최신 리콜 퀴즈 생성 작업 조회 */
    Optional<RecallQuizGeneration> findFirstByUser_UserIdAndTargetDateAndQuizTypeOrderByCreatedAtDesc(
            UUID userId,
            LocalDate targetDate,
            RecallQuizType quizType
    );

    /** 리콜 퀴즈 생성 작업 처리용 사용자 포함 조회 */
    @Query("""
            select generation
            from RecallQuizGeneration generation
            join fetch generation.user
            where generation.generationId = :generationId
            """)
    Optional<RecallQuizGeneration> findByGenerationIdWithUser(@Param("generationId") UUID generationId);
}
