package com.san.api.global.outbox.service;

import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.CommonErrorCode;
import com.san.api.global.outbox.dto.request.OutboxEventSearchRequest;
import com.san.api.global.outbox.dto.response.OutboxEventPageResponse;
import com.san.api.global.outbox.dto.response.OutboxEventResponse;
import com.san.api.global.outbox.dto.response.OutboxEventSummaryResponse;
import com.san.api.global.outbox.entity.OutboxEvent;
import com.san.api.global.outbox.repository.OutboxEventRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** 운영자가 Outbox 이벤트 상태와 실패 사유를 조회할 수 있도록 제공합니다. */
@Service
@RequiredArgsConstructor
public class OutboxEventQueryService {

    private static final int MAX_PAGE_SIZE = 100;

    private final OutboxEventRepository outboxEventRepository;

    /**
     * 조건에 맞는 Outbox 이벤트를 최신순으로 조회합니다.
     *
     * @param request 조회 조건
     * @param page    페이지 번호
     * @param size    페이지 크기
     * @return Outbox 이벤트 목록 페이지
     */
    @Transactional(readOnly = true)
    public OutboxEventPageResponse search(OutboxEventSearchRequest request, int page, int size) {
        int normalizedPage = Math.max(page, 0);
        int normalizedSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        PageRequest pageRequest = PageRequest.of(
                normalizedPage,
                normalizedSize,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        return OutboxEventPageResponse.from(outboxEventRepository
                .findAll(toSpecification(request), pageRequest)
                .map(OutboxEventSummaryResponse::from));
    }

    /**
     * Outbox 이벤트를 단건 조회합니다.
     *
     * @param outboxEventId 조회할 Outbox 이벤트 ID
     * @return Outbox 이벤트 상세 응답
     */
    @Transactional(readOnly = true)
    public OutboxEventResponse get(UUID outboxEventId) {
        return OutboxEventResponse.from(outboxEventRepository.findById(outboxEventId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND)));
    }

    private Specification<OutboxEvent> toSpecification(OutboxEventSearchRequest request) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (request.eventType() != null) {
                predicates.add(criteriaBuilder.equal(root.get("eventType"), request.eventType()));
            }
            if (request.status() != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), request.status()));
            }
            if (hasText(request.aggregateType())) {
                predicates.add(criteriaBuilder.equal(root.get("aggregateType"), request.aggregateType().trim()));
            }
            if (request.aggregateId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("aggregateId"), request.aggregateId()));
            }
            if (request.createdFrom() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), request.createdFrom()));
            }
            if (request.createdTo() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), request.createdTo()));
            }

            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
