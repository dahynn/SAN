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
        String failureCategory,
        int attemptNumber,
        boolean retryable,
        LocalDateTime createdAt,
        LocalDateTime startedAt,
        LocalDateTime completedAt
) {
    public static AsyncJobStatusResponse from(AsyncJob job) {
        return from(job, 1, false);
    }

    public static AsyncJobStatusResponse from(AsyncJob job, int attemptNumber, boolean retryable) {
        return new AsyncJobStatusResponse(
                job.getJobId(),
                job.getJobType(),
                job.getStatus(),
                safeFailureMessage(job.getErrorMessage()),
                failureCategory(job.getErrorMessage()),
                attemptNumber,
                retryable,
                job.getCreatedAt(),
                job.getStartedAt(),
                job.getCompletedAt()
        );
    }

    private static String safeFailureMessage(String errorMessage) {
        if (errorMessage == null || errorMessage.isBlank()) {
            return null;
        }
        return switch (failureCategory(errorMessage)) {
            case "TIMEOUT" -> "AI 응답 시간이 초과되었습니다. 잠시 후 다시 시도해 주세요.";
            case "EXTERNAL_SERVICE" -> "외부 AI 서비스 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.";
            default -> "AI 작업 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.";
        };
    }

    private static String failureCategory(String errorMessage) {
        if (errorMessage == null || errorMessage.isBlank()) {
            return null;
        }
        String normalized = errorMessage.toLowerCase();
        if (normalized.contains("timeout") || normalized.contains("timed out")) {
            return "TIMEOUT";
        }
        if (normalized.contains("connect") || normalized.contains("http") || normalized.contains("external")) {
            return "EXTERNAL_SERVICE";
        }
        return "PROCESSING_ERROR";
    }
}
