package com.san.api.global.async.dto.response;

import com.san.api.global.async.entity.AsyncJob;
import com.san.api.global.async.entity.JobStatus;
import com.san.api.global.async.entity.JobType;

import java.time.LocalDateTime;
import java.util.UUID;

public record AsyncJobStatusResponse(
        UUID jobId,
        JobType jobType,
        JobStatus status,
        String errorMessage,
        LocalDateTime createdAt,
        LocalDateTime startedAt,
        LocalDateTime completedAt
) {
    public static AsyncJobStatusResponse from(AsyncJob job) {
        return new AsyncJobStatusResponse(
                job.getJobId(),
                job.getJobType(),
                job.getStatus(),
                job.getErrorMessage(),
                job.getCreatedAt(),
                job.getStartedAt(),
                job.getCompletedAt()
        );
    }
}
