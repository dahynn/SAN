package com.san.api.global.logging;

import com.san.api.global.audit.context.AuditRequestContext;
import com.san.api.global.audit.context.AuditRequestContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RequestLoggingFilter가 외부 요청 헤더를 감사 로그 컨텍스트에 저장하기 전에
 * DB 컬럼 길이와 IP 형식을 만족하는 안전한 값으로 정리하는지 검증한다.
 */
class RequestLoggingFilterTest {

    private static final String TRACE_ID_HEADER = "X-Request-Id";
    private static final String FORWARDED_FOR_HEADER = "X-Forwarded-For";

    private final RequestLoggingFilter filter = new RequestLoggingFilter();

    @AfterEach
    void tearDown() {
        AuditRequestContextHolder.clear();
    }

    @Test
    void usesValidRequestHeadersForAuditContext() throws Exception {
        // 정상적인 요청 헤더는 감사 로그 컨텍스트에 그대로 사용한다.
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader(TRACE_ID_HEADER, "trace-123");
        request.addHeader(FORWARDED_FOR_HEADER, "203.0.113.10, 10.0.0.1");
        request.setRemoteAddr("10.0.0.10");
        AtomicReference<AuditRequestContext> contextRef = new AtomicReference<>();

        filter.doFilter(request, response, (servletRequest, servletResponse) ->
                contextRef.set(AuditRequestContextHolder.get().orElseThrow())
        );

        assertThat(contextRef.get().traceId()).isEqualTo("trace-123");
        assertThat(contextRef.get().ipAddress()).isEqualTo("203.0.113.10");
        assertThat(response.getHeader(TRACE_ID_HEADER)).isEqualTo("trace-123");
    }

    @Test
    void replacesInvalidTraceIdWithGeneratedUuid() throws Exception {
        // DB 컬럼 길이를 넘는 trace id는 서버에서 생성한 UUID로 대체한다.
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader(TRACE_ID_HEADER, "a".repeat(101));
        request.setRemoteAddr("10.0.0.10");
        AtomicReference<AuditRequestContext> contextRef = new AtomicReference<>();

        filter.doFilter(request, response, (servletRequest, servletResponse) ->
                contextRef.set(AuditRequestContextHolder.get().orElseThrow())
        );

        assertThat(contextRef.get().traceId()).isNotEqualTo("a".repeat(101));
        assertThatCodeAsUuid(contextRef.get().traceId());
        assertThat(response.getHeader(TRACE_ID_HEADER)).isEqualTo(contextRef.get().traceId());
    }

    @Test
    void replacesInvalidForwardedForWithRemoteAddr() throws Exception {
        // IP 형식이 아닌 X-Forwarded-For 값은 remoteAddr로 대체한다.
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader(FORWARDED_FOR_HEADER, "not-an-ip");
        request.setRemoteAddr("10.0.0.10");
        AtomicReference<AuditRequestContext> contextRef = new AtomicReference<>();

        filter.doFilter(request, response, (servletRequest, servletResponse) ->
                contextRef.set(AuditRequestContextHolder.get().orElseThrow())
        );

        assertThat(contextRef.get().ipAddress()).isEqualTo("10.0.0.10");
    }

    private void assertThatCodeAsUuid(String value) {
        assertThat(UUID.fromString(value).toString()).isEqualTo(value);
    }
}
