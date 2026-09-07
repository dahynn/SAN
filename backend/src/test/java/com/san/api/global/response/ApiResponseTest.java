package com.san.api.global.response;

import com.san.api.global.audit.context.AuditRequestContext;
import com.san.api.global.audit.context.AuditRequestContextHolder;
import com.san.api.global.exception.errorcode.CommonErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseTest {

    @AfterEach
    void tearDown() {
        AuditRequestContextHolder.clear();
    }

    @Test
    void errorIncludesCurrentTraceId() {
        AuditRequestContextHolder.set(new AuditRequestContext(
                "trace-242",
                "203.0.113.10",
                "Mozilla/5.0"
        ));

        ApiResponse<Void> response = ApiResponse.error(
                CommonErrorCode.INVALID_INPUT_VALUE,
                CommonErrorCode.INVALID_INPUT_VALUE.getMessage()
        );

        assertThat(response.ok()).isFalse();
        assertThat(response.traceId()).isEqualTo("trace-242");
    }

    @Test
    void successDoesNotIncludeTraceId() {
        AuditRequestContextHolder.set(new AuditRequestContext(
                "trace-242",
                "203.0.113.10",
                "Mozilla/5.0"
        ));

        ApiResponse<Void> response = ApiResponse.success();

        assertThat(response.ok()).isTrue();
        assertThat(response.traceId()).isNull();
    }
}
