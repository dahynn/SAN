package com.san.api.global.async.repository;

import com.san.api.global.async.entity.AsyncJob;
import com.san.api.global.async.entity.JobStatus;
import com.san.api.global.async.entity.JobType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface AsyncJobRepository extends JpaRepository<AsyncJob, UUID> {

    List<AsyncJob> findByTargetIdAndJobType(UUID targetId, JobType jobType);

    List<AsyncJob> findByStatus(JobStatus status);

    List<AsyncJob> findByStatusAndCreatedAtBefore(JobStatus status, LocalDateTime threshold);

    long countByTargetIdAndJobTypeAndStatus(UUID targetId, JobType jobType, JobStatus status);

    boolean existsByTargetIdAndJobTypeAndStatusIn(UUID targetId, JobType jobType, List<JobStatus> statuses);

    @Query("""
            SELECT job FROM AsyncJob job
            WHERE job.targetId = :targetId
              AND job.jobType = :jobType
              AND job.auditContext.actorUserId = :userId
            ORDER BY job.createdAt DESC
            """)
    List<AsyncJob> findHistoryForUser(
            @Param("targetId") UUID targetId,
            @Param("jobType") JobType jobType,
            @Param("userId") UUID userId
    );
}
