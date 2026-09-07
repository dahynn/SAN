package com.san.api.global.outbox.dto.response;

import org.springframework.data.domain.Page;

import java.util.List;

/** Outbox 이벤트 운영 목록 페이지 응답입니다. */
public record OutboxEventPageResponse(
        List<OutboxEventSummaryResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public static OutboxEventPageResponse from(Page<OutboxEventSummaryResponse> page) {
        return new OutboxEventPageResponse(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
