package com.san.api.global.audit.service;

import com.san.api.global.audit.dto.request.AuditLogSearchRequest;
import com.san.api.global.audit.dto.response.AuditLogPageResponse;
import com.san.api.global.audit.dto.response.AuditLogResponse;
import com.san.api.global.audit.entity.AuditLogEvent;
import com.san.api.global.audit.repository.AuditLogEventRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditLogQueryService {

    private static final int MAX_PAGE_SIZE = 100;

    private final AuditLogEventRepository auditLogEventRepository;

    @Transactional(readOnly = true)
    public AuditLogPageResponse search(AuditLogSearchRequest request, int page, int size) {
        int normalizedPage = Math.max(page, 0);
        int normalizedSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        PageRequest pageRequest = PageRequest.of(
                normalizedPage,
                normalizedSize,
                Sort.by(Sort.Direction.DESC, "occurredAt")
        );

        return AuditLogPageResponse.from(auditLogEventRepository
                .findAll(toSpecification(request), pageRequest)
                .map(AuditLogResponse::from));
    }

    private Specification<AuditLogEvent> toSpecification(AuditLogSearchRequest request) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (request.actorUserId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("actorUserId"), request.actorUserId()));
            }
            if (hasText(request.traceId())) {
                predicates.add(criteriaBuilder.equal(root.get("traceId"), request.traceId().trim()));
            }
            if (request.eventDomain() != null) {
                predicates.add(criteriaBuilder.equal(root.get("eventDomain"), request.eventDomain()));
            }
            if (request.eventType() != null) {
                predicates.add(criteriaBuilder.equal(root.get("eventType"), request.eventType()));
            }
            if (request.outcome() != null) {
                predicates.add(criteriaBuilder.equal(root.get("outcome"), request.outcome()));
            }
            if (hasText(request.targetType())) {
                predicates.add(criteriaBuilder.equal(root.get("targetType"), request.targetType().trim()));
            }
            if (request.targetId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("targetId"), request.targetId()));
            }
            if (hasText(request.failureReasonCode())) {
                predicates.add(criteriaBuilder.equal(root.get("failureReasonCode"), request.failureReasonCode().trim()));
            }
            if (request.from() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("occurredAt"), request.from()));
            }
            if (request.to() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("occurredAt"), request.to()));
            }

            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
