package com.san.api.global.audit.service;

import com.san.api.global.audit.dto.response.AuditLogIntegrityResponse;
import com.san.api.global.audit.dto.response.AuditLogIntegritySummaryResponse;
import com.san.api.global.audit.entity.AuditIntegrityStatus;
import com.san.api.global.audit.entity.AuditLogEvent;
import com.san.api.global.audit.repository.AuditLogEventRepository;
import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.CommonErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditLogIntegrityService {

    private static final int MAX_VERIFY_SIZE = 500;

    private final AuditLogEventRepository auditLogEventRepository;
    private final AuditLogIntegrityHasher auditLogIntegrityHasher;

    /**
     * 현재 사용자에게 속한 감사 로그 단건의 저장된 해시와 현재 로그 내용으로 다시 계산한 해시를 비교합니다.
     *
     * @param auditLogEventId 검증할 감사 로그 식별자
     * @param actorUserId 현재 사용자 식별자
     * @return 단건 무결성 검증 결과
     */
    @Transactional(readOnly = true)
    public AuditLogIntegrityResponse verify(UUID auditLogEventId) {
        AuditLogEvent event = auditLogEventRepository.findById(auditLogEventId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND));
        return response(event, Instant.now());
    }

    /**
     * 현재 사용자에게 속한 지정 기간의 감사 로그를 발생 시각 오름차순으로 조회해 페이지 단위로 무결성을 검증합니다.
     * 한 번에 과도한 범위를 검증하지 않도록 요청 크기는 최대 500건으로 제한합니다.
     *
     * @param from 검증 시작 시각
     * @param to 검증 종료 시각
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @param actorUserId 현재 사용자 식별자
     * @return 요청 페이지 기준 감사 로그 무결성 검증 요약
     */
    @Transactional(readOnly = true)
    public AuditLogIntegritySummaryResponse verifyRange(
            LocalDateTime from,
            LocalDateTime to,
            int page,
            int size
    ) {
        if (from.isAfter(to)) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT_VALUE);
        }
        int normalizedPage = Math.max(page, 0);
        int normalizedSize = Math.min(Math.max(size, 1), MAX_VERIFY_SIZE);
        Page<AuditLogEvent> events = auditLogEventRepository.findAll(
                        occurredBetween(from, to),
                        PageRequest.of(normalizedPage, normalizedSize, Sort.by(Sort.Direction.ASC, "occurredAt"))
                );
        Instant verifiedAt = Instant.now();
        List<AuditLogIntegrityResponse> results = events
                .map(event -> response(event, verifiedAt))
                .getContent();

        List<UUID> invalidEventIds = results.stream()
                .filter(result -> result.status() == AuditIntegrityStatus.INVALID)
                .map(AuditLogIntegrityResponse::auditLogEventId)
                .toList();
        List<UUID> missingHashEventIds = results.stream()
                .filter(result -> result.status() == AuditIntegrityStatus.MISSING_HASH)
                .map(AuditLogIntegrityResponse::auditLogEventId)
                .toList();

        return new AuditLogIntegritySummaryResponse(
                normalizedPage,
                normalizedSize,
                events.getTotalElements(),
                events.getTotalPages(),
                events.hasNext(),
                results.size(),
                (int) results.stream().filter(AuditLogIntegrityResponse::valid).count(),
                invalidEventIds.size(),
                missingHashEventIds.size(),
                invalidEventIds,
                missingHashEventIds,
                verifiedAt
        );
    }

    private AuditLogIntegrityResponse response(AuditLogEvent event, Instant verifiedAt) {
        AuditIntegrityStatus status = status(event);
        return new AuditLogIntegrityResponse(
                event.getAuditLogEventId(),
                status,
                status.getDescription(),
                status == AuditIntegrityStatus.VALID,
                verifiedAt
        );
    }

    private AuditIntegrityStatus status(AuditLogEvent event) {
        if (event.getIntegrityHash() == null || event.getIntegrityHash().isBlank()) {
            // 무결성 해시 도입 전 생성된 기존 감사 로그는 조작이 아니라 미검증 대상으로 분리합니다.
            return AuditIntegrityStatus.MISSING_HASH;
        }
        String calculatedHash = auditLogIntegrityHasher.hash(event);
        if (event.getIntegrityHash().equals(calculatedHash)) {
            return AuditIntegrityStatus.VALID;
        }
        return AuditIntegrityStatus.INVALID;
    }

    private Specification<AuditLogEvent> occurredBetween(LocalDateTime from, LocalDateTime to) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.and(
                criteriaBuilder.greaterThanOrEqualTo(root.get("occurredAt"), from),
                criteriaBuilder.lessThanOrEqualTo(root.get("occurredAt"), to)
        );
    }

}
