package com.san.api.global.audit.service;

import com.san.api.global.audit.dto.request.AuditLogSearchRequest;
import com.san.api.global.audit.dto.response.AuditLogPageResponse;
import com.san.api.global.audit.entity.AuditEventDomain;
import com.san.api.global.audit.entity.AuditEventType;
import com.san.api.global.audit.entity.AuditLogEvent;
import com.san.api.global.audit.entity.AuditOutcome;
import com.san.api.global.audit.repository.AuditLogEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class AuditLogQueryServiceTest {

    @Mock
    private AuditLogEventRepository auditLogEventRepository;

    @Test
    void searchReturnsAuditLogsWithNewestFirstPageRequest() {
        AuditLogQueryService service = new AuditLogQueryService(auditLogEventRepository);
        AuditLogEvent event = AuditLogEvent.builder()
                .traceId("trace-137")
                .eventDomain(AuditEventDomain.AUTH)
                .eventType(AuditEventType.LOGIN_FAILURE)
                .targetType("USER")
                .outcome(AuditOutcome.FAILURE)
                .failureReasonCode("AUTH.INVALID_CREDENTIALS")
                .failureMessage("아이디 또는 비밀번호가 올바르지 않습니다.")
                .ipAddress("203.0.113.10")
                .userAgent("Mozilla/5.0")
                .metadata(Map.of("sourceClientType", "DASHBOARD"))
                .build();

        when(auditLogEventRepository.findAll(
                any(Specification.class),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(event)));

        AuditLogPageResponse response = service.search(
                new AuditLogSearchRequest(
                        null,
                        " trace-137 ",
                        AuditEventDomain.AUTH,
                        AuditEventType.LOGIN_FAILURE,
                        AuditOutcome.FAILURE,
                        "USER",
                        null,
                        "AUTH.INVALID_CREDENTIALS",
                        null,
                        null
                ),
                0,
                20
        );

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).traceId()).isEqualTo("trace-137");
        assertThat(response.content().get(0).failureReasonCode()).isEqualTo("AUTH.INVALID_CREDENTIALS");
        assertThat(response.content().get(0).metadata()).containsEntry("sourceClientType", "DASHBOARD");

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(auditLogEventRepository).findAll(any(Specification.class), pageableCaptor.capture());
        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(20);
        assertThat(pageable.getSort().getOrderFor("occurredAt")).isEqualTo(
                new Sort.Order(Sort.Direction.DESC, "occurredAt")
        );
    }

    @Test
    void searchClampsPageSizeToMaximum() {
        AuditLogQueryService service = new AuditLogQueryService(auditLogEventRepository);
        when(auditLogEventRepository.findAll(
                any(Specification.class),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of()));

        service.search(new AuditLogSearchRequest(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        ), -1, 500);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(auditLogEventRepository).findAll(any(Specification.class), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageNumber()).isZero();
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(100);
    }
}
