package com.san.api.global.audit.controller;

import com.san.api.global.audit.dto.request.AuditLogSearchRequest;
import com.san.api.global.audit.dto.response.AuditLogPageResponse;
import com.san.api.global.audit.dto.response.AuditLogSummaryResponse;
import com.san.api.global.audit.service.AuditLogQueryService;
import com.san.api.global.audit.service.AuditLogSummaryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditLogControllerTest {

    @Mock
    private AuditLogQueryService auditLogQueryService;
    @Mock
    private AuditLogSummaryService auditLogSummaryService;

    @Test
    void searchUsesRequestedConditionsForAdminQuery() {
        AuditLogController controller = new AuditLogController(auditLogQueryService, auditLogSummaryService);
        UUID requestedActorUserId = UUID.randomUUID();
        AuditLogSearchRequest request = new AuditLogSearchRequest(
                requestedActorUserId,
                "trace-137",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
        AuditLogPageResponse response = new AuditLogPageResponse(List.of(), 0, 20, 0, 0);
        when(auditLogQueryService.search(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq(0),
                org.mockito.ArgumentMatchers.eq(20)
        )).thenReturn(response);

        controller.search(request, 0, 20);

        ArgumentCaptor<AuditLogSearchRequest> captor = ArgumentCaptor.forClass(AuditLogSearchRequest.class);
        verify(auditLogQueryService).search(
                captor.capture(),
                org.mockito.ArgumentMatchers.eq(0),
                org.mockito.ArgumentMatchers.eq(20)
        );
        assertThat(captor.getValue().actorUserId()).isEqualTo(requestedActorUserId);
        assertThat(captor.getValue().traceId()).isEqualTo("trace-137");
    }

    @Test
    void summaryUsesRequestedTimeRange() {
        AuditLogController controller = new AuditLogController(auditLogQueryService, auditLogSummaryService);
        LocalDateTime from = LocalDateTime.of(2026, 5, 27, 9, 0);
        LocalDateTime to = LocalDateTime.of(2026, 5, 27, 15, 0);
        AuditLogSummaryResponse response = new AuditLogSummaryResponse(
                from,
                to,
                10,
                9,
                1,
                new BigDecimal("10.00"),
                List.of(),
                new AuditLogSummaryResponse.AsyncJobSummaryResponse(3, 2, 1, 3, new BigDecimal("33.33")),
                List.of(),
                List.of()
        );
        when(auditLogSummaryService.summarize(from, to)).thenReturn(response);

        controller.summary(from, to);

        verify(auditLogSummaryService).summarize(from, to);
    }
}
