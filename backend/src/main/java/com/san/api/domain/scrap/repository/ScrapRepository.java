package com.san.api.domain.scrap.repository;

import com.san.api.domain.scrap.entity.Scrap;
import com.san.api.domain.scrap.entity.SourceType;
import com.san.api.global.async.entity.JobStatus;
import com.san.api.global.async.entity.JobType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** 스크랩 엔티티 Repository */
public interface ScrapRepository extends JpaRepository<Scrap, UUID> {

    /** 사용자, 원본 유형, 원본 해시 기준 스크랩 조회 */
    Optional<Scrap> findByUser_UserIdAndSourceTypeAndContentHash(UUID userId, SourceType sourceType, String contentHash);

    /**
     * 특정 날짜에 수집된 스크랩의 지식 카드 ID 목록 조회.
     * TIL 리콜 추천 시 원본 카드를 제외하기 위해 사용.
     */
    @Query(value = """
            SELECT kc.card_id
            FROM scraps s
            JOIN knowledge_cards kc ON kc.scrap_id = s.scrap_id
            WHERE s.user_id = :userId
              AND CAST(s.created_at AS date) = :targetDate
              AND s.is_deleted = false
              AND kc.is_deleted = false
            """, nativeQuery = true)
    List<UUID> findCardIdsByUserAndDate(
            @Param("userId") UUID userId,
            @Param("targetDate") LocalDate targetDate
    );

    /** 
     * 지식카드가 없고 활성 CARD_ANALYSIS 잡도 없는 고아 스크랩 조회.
     * Ghost Job 복구 대상(PENDING/PROCESSING 잡 존재)과 겹치지 않도록 활성 잡 조건으로 제외한다.
     */
    @Query("""
            SELECT s FROM Scrap s
            WHERE s.isDeleted = false
            AND NOT EXISTS (
                SELECT kc FROM KnowledgeCard kc WHERE kc.scrap = s
            )
            AND NOT EXISTS (
                SELECT aj FROM AsyncJob aj
                WHERE aj.targetId = s.scrapId
                  AND aj.jobType = :jobType
                  AND aj.status IN :activeStatuses
            )
            """)
    List<Scrap> findOrphanScraps(
            @Param("jobType") JobType jobType,
            @Param("activeStatuses") List<JobStatus> activeStatuses
    );

    /**
     * refinedContent가 없고 활성 SCRAP_REFINE 잡도 없는 고아 스크랩 조회.
     * Ghost Job 복구 대상(PENDING/PROCESSING 잡 존재)과 겹치지 않도록 활성 잡 조건으로 제외한다.
     */
    @Query("""
            SELECT s FROM Scrap s
            WHERE s.isDeleted = false
              AND (s.refinedContent IS NULL OR TRIM(s.refinedContent) = '')
              AND NOT EXISTS (
                  SELECT aj FROM AsyncJob aj
                  WHERE aj.targetId = s.scrapId
                    AND aj.jobType = :jobType
                    AND aj.status IN :activeStatuses
              )
            """)
    List<Scrap> findOrphanScrapRefines(
            @Param("jobType") JobType jobType,
            @Param("activeStatuses") List<JobStatus> activeStatuses
    );

}
