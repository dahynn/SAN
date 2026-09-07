package com.san.api.domain.github.repository;

import com.san.api.domain.github.entity.GithubStarRecommendation;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** GitHub Star 추천 후보 Repository */
public interface GithubStarRecommendationRepository extends JpaRepository<GithubStarRecommendation, UUID> {

    // 사용자 추천 후보 존재 여부 조회
    boolean existsByUser_UserId(UUID userId);

    // 사용자 추천 후보 목록 조회
    List<GithubStarRecommendation> findAllByUser_UserIdOrderByCreatedAtDesc(UUID userId);

    // 비동기 작업 추천 후보 목록 조회
    List<GithubStarRecommendation> findAllByJobIdOrderByCreatedAtAsc(UUID jobId);

    // 사용자 추천 후보 단건 조회
    Optional<GithubStarRecommendation> findByGithubStarRecommendationIdAndUser_UserId(
            UUID githubStarRecommendationId,
            UUID userId
    );

    // 사용자 추천 후보 단건 조회 및 수집 처리 잠금
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT recommendation
            FROM GithubStarRecommendation recommendation
            WHERE recommendation.githubStarRecommendationId = :recommendationId
              AND recommendation.user.userId = :userId
            """)
    Optional<GithubStarRecommendation> findByIdAndUserIdForUpdate(
            @Param("recommendationId") UUID recommendationId,
            @Param("userId") UUID userId
    );
}
