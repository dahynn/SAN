package com.san.api.global.async;

import com.san.api.global.async.dto.response.AsyncJobStatusResponse;
import com.san.api.global.async.entity.AsyncJob;
import com.san.api.global.async.entity.JobType;
import com.san.api.global.audit.context.AuditContextSnapshot;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AsyncJobStatusResponseTest {

    @Test
    void from_exposesSafeFailureCategoryInsteadOfRawExceptionMessage() {
        AsyncJob job = AsyncJob.builder()
                .jobType(JobType.TIL_GENERATION)
                .targetId(UUID.randomUUID())
                .auditContext(AuditContextSnapshot.empty())
                .build();
        job.fail("SocketTimeoutException: secret upstream diagnostic");

        AsyncJobStatusResponse response = AsyncJobStatusResponse.from(job, 2, true);

        assertThat(response.failureCategory()).isEqualTo("TIMEOUT");
        assertThat(response.errorMessage()).contains("응답 시간이 초과").doesNotContain("secret upstream diagnostic");
        assertThat(response.attemptNumber()).isEqualTo(2);
        assertThat(response.retryable()).isTrue();
    }
}
