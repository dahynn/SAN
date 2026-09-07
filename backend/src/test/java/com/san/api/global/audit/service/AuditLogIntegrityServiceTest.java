package com.san.api.global.audit.service;

import com.san.api.global.audit.dto.response.AuditLogIntegrityResponse;
import com.san.api.global.audit.dto.response.AuditLogIntegritySummaryResponse;
import com.san.api.global.audit.entity.AuditEventDomain;
import com.san.api.global.audit.entity.AuditEventType;
import com.san.api.global.audit.entity.AuditIntegrityStatus;
import com.san.api.global.audit.entity.AuditLogEvent;
import com.san.api.global.audit.entity.AuditOutcome;
import com.san.api.global.audit.repository.AuditLogEventRepository;
import com.san.api.global.config.ObjectMapperConfig;
import com.san.api.global.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class AuditLogIntegrityServiceTest {

    private static final UUID ACTOR_USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Mock
    private AuditLogEventRepository auditLogEventRepository;

    private final AuditLogIntegrityHasher hasher = new AuditLogIntegrityHasher(new ObjectMapperConfig().objectMapper());

    @Test
    void verifyReturnsValidWhenStoredHashMatchesCalculatedHash() {
        AuditLogEvent event = event();
        event.updateIntegrityHash(hasher.hash(event));
        AuditLogIntegrityService service = new AuditLogIntegrityService(auditLogEventRepository, hasher);
        when(auditLogEventRepository.findById(event.getAuditLogEventId())).thenReturn(Optional.of(event));

        AuditLogIntegrityResponse response = service.verify(event.getAuditLogEventId());

        assertThat(response.status()).isEqualTo(AuditIntegrityStatus.VALID);
        assertThat(response.statusDescription()).isEqualTo(AuditIntegrityStatus.VALID.getDescription());
        assertThat(response.valid()).isTrue();
    }

    @Test
    void verifyReturnsInvalidWhenStoredHashDoesNotMatchCalculatedHash() {
        AuditLogEvent event = event();
        event.updateIntegrityHash("0".repeat(64));
        AuditLogIntegrityService service = new AuditLogIntegrityService(auditLogEventRepository, hasher);
        when(auditLogEventRepository.findById(event.getAuditLogEventId())).thenReturn(Optional.of(event));

        AuditLogIntegrityResponse response = service.verify(event.getAuditLogEventId());

        assertThat(response.status()).isEqualTo(AuditIntegrityStatus.INVALID);
        assertThat(response.valid()).isFalse();
    }

    @Test
    void verifyAllowsAdminToCheckEventOwnedByAnotherActor() {
        AuditLogEvent event = event();
        event.updateIntegrityHash(hasher.hash(event));
        AuditLogIntegrityService service = new AuditLogIntegrityService(auditLogEventRepository, hasher);
        when(auditLogEventRepository.findById(event.getAuditLogEventId())).thenReturn(Optional.of(event));

        AuditLogIntegrityResponse response = service.verify(event.getAuditLogEventId());

        assertThat(response.status()).isEqualTo(AuditIntegrityStatus.VALID);
    }

    @Test
    void verifyRangeSummarizesInvalidAndMissingHashEvents() {
        AuditLogEvent valid = event();
        valid.updateIntegrityHash(hasher.hash(valid));
        AuditLogEvent invalid = event();
        invalid.updateIntegrityHash("0".repeat(64));
        AuditLogEvent missing = event();
        AuditLogIntegrityService service = new AuditLogIntegrityService(auditLogEventRepository, hasher);
        when(auditLogEventRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(valid, invalid, missing), Pageable.ofSize(500), 501));

        AuditLogIntegritySummaryResponse response = service.verifyRange(
                LocalDateTime.of(2026, 5, 18, 0, 0),
                LocalDateTime.of(2026, 5, 18, 23, 59),
                0,
                1_000
        );

        assertThat(response.page()).isZero();
        assertThat(response.size()).isEqualTo(500);
        assertThat(response.totalMatchedCount()).isEqualTo(501);
        assertThat(response.totalPages()).isEqualTo(2);
        assertThat(response.hasNext()).isTrue();
        assertThat(response.checkedCount()).isEqualTo(3);
        assertThat(response.validCount()).isEqualTo(1);
        assertThat(response.invalidCount()).isEqualTo(1);
        assertThat(response.missingHashCount()).isEqualTo(1);
        assertThat(response.invalidEventIds()).containsExactly(invalid.getAuditLogEventId());
        assertThat(response.missingHashEventIds()).containsExactly(missing.getAuditLogEventId());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(auditLogEventRepository).findAll(any(Specification.class), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(500);
    }

    @Test
    void verifyRangeRejectsInvalidRange() {
        AuditLogIntegrityService service = new AuditLogIntegrityService(auditLogEventRepository, hasher);

        assertThatThrownBy(() -> service.verifyRange(
                LocalDateTime.of(2026, 5, 19, 0, 0),
                LocalDateTime.of(2026, 5, 18, 0, 0),
                0,
                100
        )).isInstanceOf(BusinessException.class);
    }

    private AuditLogEvent event() {
        AuditLogEvent event = AuditLogEvent.builder()
                .traceId("trace-integrity")
                .eventDomain(AuditEventDomain.AUTH)
                .eventType(AuditEventType.LOGIN_FAILURE)
                .targetType("USER")
                .outcome(AuditOutcome.FAILURE)
                .failureReasonCode("AUTH.INVALID_CREDENTIALS")
                .failureMessage("아이디 또는 비밀번호가 올바르지 않습니다.")
                .ipAddress("203.0.113.10")
                .userAgent("Mozilla/5.0")
                .metadata(Map.of("clientType", "DASHBOARD"))
                .build();
        ReflectionTestUtils.setField(event, "occurredAt", LocalDateTime.of(2026, 5, 18, 13, 30));
        ReflectionTestUtils.setField(event, "actorUserId", ACTOR_USER_ID);
        return event;
    }
}
