package com.san.api.global.exception;

import com.san.api.global.audit.context.AuditRequestContext;
import com.san.api.global.audit.context.AuditRequestContextHolder;
import com.san.api.global.exception.errorcode.CommonErrorCode;
import com.san.api.global.response.ApiResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @AfterEach
    void tearDown() {
        AuditRequestContextHolder.clear();
    }

    @Test
    void businessExceptionResponseIncludesTraceId() {
        AuditRequestContextHolder.set(new AuditRequestContext(
                "trace-global-242",
                "203.0.113.10",
                "Mozilla/5.0"
        ));

        ResponseEntity<ApiResponse<Void>> response = handler.handleBusinessException(
                new BusinessException(CommonErrorCode.BAD_REQUEST)
        );

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().traceId()).isEqualTo("trace-global-242");
    }
}
