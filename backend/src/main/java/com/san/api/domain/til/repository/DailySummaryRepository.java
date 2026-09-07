package com.san.api.domain.til.repository;

import com.san.api.domain.til.entity.DailySummary;
import com.san.api.global.async.entity.JobStatus;
import com.san.api.global.async.entity.JobType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** DailySummary Repository */
public interface DailySummaryRepository extends JpaRepository<DailySummary, UUID> {

    // 사용자 기준 전체 TIL 개수 조회
    long countByUser_UserId(UUID userId);

    @Query(value = """
            SELECT pg_advisory_xact_lock(
                ('x' || substr(md5(CAST(:userId AS text)), 1, 16))::bit(64)::bigint
            )
            """, nativeQuery = true)
    void acquireGenerationLock(@Param("userId") UUID userId);

    /**
     * 사용자와 대상 날짜 기준 DailySummary 조회
     *
     * @param userId 사용자 ID
     * @param targetDate TIL 대상 날짜
     * @return 조회된 DailySummary 목록
     */
    List<DailySummary> findAllByUser_UserIdAndTargetDateOrderByCreatedAtDesc(UUID userId, LocalDate targetDate);

    /**
     * 사용자와 대상 날짜 기준 최신 DailySummary 조회
     *
     * @param userId 사용자 ID
     * @param targetDate TIL 대상 날짜
     * @return 사용자 정보가 포함된 DailySummary 목록
     */
    @Query("""
            SELECT ds
            FROM DailySummary ds
            JOIN FETCH ds.user
            WHERE ds.user.userId = :userId
              AND ds.targetDate = :targetDate
            ORDER BY ds.createdAt DESC
            """)
    List<DailySummary> findAllByUserIdAndTargetDateWithUserOrderByCreatedAtDesc(
            @Param("userId") UUID userId,
            @Param("targetDate") LocalDate targetDate
    );

    /**
     * 사용자 정보를 함께 조회하는 DailySummary 단건 조회
     *
     * @param summaryId DailySummary ID
     * @return 사용자 정보가 포함된 DailySummary
     */
    @Query("""
            SELECT ds
            FROM DailySummary ds
            JOIN FETCH ds.user
            WHERE ds.summaryId = :summaryId
            """)
    Optional<DailySummary> findBySummaryIdWithUser(@Param("summaryId") UUID summaryId);

    @Query("""
            SELECT COUNT(ds) > 0
            FROM DailySummary ds
            JOIN AsyncJob aj ON aj.targetId = ds.summaryId
            WHERE ds.user.userId = :userId
              AND aj.jobType = :jobType
              AND aj.status IN :statuses
            """)
    boolean existsActiveJobForUser(
            @Param("userId") UUID userId,
            @Param("jobType") JobType jobType,
            @Param("statuses") Collection<JobStatus> statuses
    );
}
