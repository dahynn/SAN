package com.san.api.global.security.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.san.api.global.config.ObjectMapperConfig;
import com.san.api.global.audit.context.AuditRequestContext;
import com.san.api.global.audit.context.AuditRequestContextHolder;
import com.san.api.global.exception.errorcode.CommonErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityErrorResponseWriterTest {

    private final ObjectMapper objectMapper = new ObjectMapperConfig().objectMapper();
    private final SecurityErrorResponseWriter writer = new SecurityErrorResponseWriter(objectMapper);

    @AfterEach
    void tearDown() {
        AuditRequestContextHolder.clear();
    }

    @Test
    void securityErrorResponseIncludesTraceId() throws Exception {
        AuditRequestContextHolder.set(new AuditRequestContext(
                "trace-security-242",
                "203.0.113.10",
                "Mozilla/5.0"
        ));
        MockHttpServletResponse response = new MockHttpServletResponse();

        writer.write(response, CommonErrorCode.UNAUTHORIZED);

        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertThat(body.get("traceId").asText()).isEqualTo("trace-security-242");
    }
}
