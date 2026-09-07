package com.san.api.domain.til.repository;

import com.san.api.domain.til.entity.TilGithubCommit;
import com.san.api.domain.til.entity.TilGithubCommitStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** TIL GitHub 커밋 이력을 조회하는 repository */
public interface TilGithubCommitRepository extends JpaRepository<TilGithubCommit, UUID> {

    @Query("""
            SELECT COUNT(tgc) > 0
            FROM TilGithubCommit tgc
            WHERE tgc.githubRepositoryConnection.githubRepositoryConnectionId = :githubRepositoryConnectionId
              AND tgc.branch = :branch
              AND tgc.contentHash = :contentHash
              AND tgc.status IN :statuses
            """)
    boolean existsDuplicateContent(
            @Param("githubRepositoryConnectionId") UUID githubRepositoryConnectionId,
            @Param("branch") String branch,
            @Param("contentHash") String contentHash,
            @Param("statuses") Collection<TilGithubCommitStatus> statuses
    );

    @Query("""
            SELECT tgc
            FROM TilGithubCommit tgc
            JOIN FETCH tgc.dailySummary ds
            JOIN FETCH ds.user
            JOIN FETCH tgc.githubRepositoryConnection
            WHERE tgc.tilGithubCommitId = :commitId
            """)
    Optional<TilGithubCommit> findByIdWithSummaryAndRepository(@Param("commitId") UUID commitId);

    /** contribution 잔디 집계를 위해 기간 내 완료된 TIL GitHub 커밋 이력을 조회합니다. */
    @Query("""
            SELECT tgc
            FROM TilGithubCommit tgc
            JOIN FETCH tgc.dailySummary ds
            JOIN FETCH tgc.githubRepositoryConnection grc
            WHERE ds.user.userId = :userId
              AND tgc.status = :status
              AND tgc.pushedAt >= :from
              AND tgc.pushedAt < :to
              AND (:githubRepositoryId IS NULL OR grc.githubRepositoryId = :githubRepositoryId)
            ORDER BY tgc.pushedAt DESC
            """)
    List<TilGithubCommit> findContributions(
            @Param("userId") UUID userId,
            @Param("status") TilGithubCommitStatus status,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("githubRepositoryId") Long githubRepositoryId
    );
}
