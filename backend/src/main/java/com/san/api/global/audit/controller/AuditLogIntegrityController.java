package com.san.api.global.audit.controller;

import com.san.api.global.audit.dto.response.AuditLogIntegrityResponse;
import com.san.api.global.audit.dto.response.AuditLogIntegritySummaryResponse;
import com.san.api.global.audit.service.AuditLogIntegrityService;
import com.san.api.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 감사 로그 무결성 검증 API를 제공합니다.
 */
@Tag(name = "Audit Log Integrity", description = "감사 로그 무결성 검증 API")
@Validated
@RestController
@RequestMapping("/admin/audit-logs")
@RequiredArgsConstructor
public class AuditLogIntegrityController {

    private final AuditLogIntegrityService auditLogIntegrityService;

    /**
     * 감사 로그 단건의 무결성 상태를 검증합니다.
     *
     * @param authentication 현재 로그인 사용자 인증 정보
     * @param auditLogEventId 검증할 감사 로그 식별자
     * @return 저장된 해시와 현재 로그 내용의 일치 여부
     */
    @Operation(summary = "감사 로그 단건 무결성 검증")
    @GetMapping("/{auditLogEventId}/integrity")
    public ApiResponse<AuditLogIntegrityResponse> verify(@PathVariable UUID auditLogEventId) {
        return ApiResponse.success(auditLogIntegrityService.verify(auditLogEventId));
    }

    /**
     * 지정한 기간의 감사 로그 무결성 상태를 페이지 단위로 검증합니다.
     *
     * @param authentication 현재 로그인 사용자 인증 정보
     * @param from 검증 시작 시각
     * @param to 검증 종료 시각
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 요청 페이지 기준 유효, 불일치, 해시 누락 건수와 대상 식별자 목록
     */
    @Operation(summary = "감사 로그 기간 무결성 검증")
    @GetMapping("/integrity")
    public ApiResponse<AuditLogIntegritySummaryResponse> verifyRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "100") @Min(1) @Max(500) int size
    ) {
        return ApiResponse.success(auditLogIntegrityService.verifyRange(
                from,
                to,
                page,
                size
        ));
    }
}
