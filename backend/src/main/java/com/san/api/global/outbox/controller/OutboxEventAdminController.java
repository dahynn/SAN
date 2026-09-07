package com.san.api.global.outbox.controller;

import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.CommonErrorCode;
import com.san.api.global.outbox.dto.request.OutboxEventSearchRequest;
import com.san.api.global.outbox.dto.response.OutboxEventPageResponse;
import com.san.api.global.outbox.dto.response.OutboxEventResponse;
import com.san.api.global.outbox.service.OutboxEventQueryService;
import com.san.api.global.outbox.service.OutboxEventRetryService;
import com.san.api.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Outbox Admin", description = "Outbox 이벤트 운영 API")
@Validated
@RestController
@RequestMapping("/admin/outbox-events")
@RequiredArgsConstructor
public class OutboxEventAdminController {

    private final OutboxEventQueryService outboxEventQueryService;
    private final OutboxEventRetryService outboxEventRetryService;

    /**
     * Outbox 이벤트를 조건별로 조회합니다.
     *
     * @param request 조회 조건
     * @param page    페이지 번호
     * @param size    페이지 크기
     * @return Outbox 이벤트 목록
     */
    @Operation(summary = "Outbox 이벤트 운영 조회", description = "Outbox 이벤트 상태와 실패 사유를 조건별로 조회합니다.")
    @GetMapping
    public ApiResponse<OutboxEventPageResponse> search(
            @ModelAttribute OutboxEventSearchRequest request,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ApiResponse.success(outboxEventQueryService.search(request, page, size));
    }

    /**
     * Outbox 이벤트를 단건 조회합니다.
     *
     * @param outboxEventId 조회할 Outbox 이벤트 ID
     * @return Outbox 이벤트 상세
     */
    @Operation(summary = "Outbox 이벤트 상세 조회", description = "Outbox 이벤트 payload와 처리 상태를 단건 조회합니다.")
    @GetMapping("/{outboxEventId}")
    public ApiResponse<OutboxEventResponse> get(@PathVariable UUID outboxEventId) {
        return ApiResponse.success(outboxEventQueryService.get(outboxEventId));
    }

    /**
     * FAILED 상태의 Outbox 이벤트를 즉시 재처리 대기 상태로 변경합니다.
     *
     * @param outboxEventId  재처리할 Outbox 이벤트 ID
     * @param authentication 현재 인증 정보
     * @return 재처리 대기 상태로 변경된 Outbox 이벤트
     */
    @Operation(
            summary = "실패 Outbox 이벤트 수동 재처리",
            description = "FAILED 상태의 Outbox 이벤트를 PENDING 상태로 되돌려 릴레이 스케줄러가 다시 처리하게 합니다."
    )
    @PostMapping("/{outboxEventId}/retry")
    public ApiResponse<OutboxEventResponse> retry(
            @PathVariable UUID outboxEventId,
            Authentication authentication
    ) {
        return ApiResponse.success(outboxEventRetryService.retryFailedEvent(
                outboxEventId,
                currentUserId(authentication)
        ));
    }

    private UUID currentUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED);
        }
        return UUID.fromString((String) authentication.getPrincipal());
    }
}
