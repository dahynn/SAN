package com.san.api.global.audit.controller;

import com.san.api.global.audit.dto.request.AuditLogSearchRequest;
import com.san.api.global.audit.dto.response.AuditLogPageResponse;
import com.san.api.global.audit.dto.response.AuditLogSummaryResponse;
import com.san.api.global.audit.service.AuditLogQueryService;
import com.san.api.global.audit.service.AuditLogSummaryService;
import com.san.api.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@Tag(name = "Audit Log", description = "감사 로그 API")
@Validated
@RestController
@RequestMapping("/admin/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogQueryService auditLogQueryService;
    private final AuditLogSummaryService auditLogSummaryService;

    /**
     * 관리자가 감사 로그를 조건별로 조회합니다.
     *
     * @param request 조회 조건
     * @param page    페이지 번호
     * @param size    페이지 크기
     * @return 감사 로그 목록
     */
    @Operation(summary = "감사 로그 조회", description = "관리자가 감사 로그를 조건별로 최신순 조회합니다.")
    @GetMapping
    public ApiResponse<AuditLogPageResponse> search(
            @ModelAttribute AuditLogSearchRequest request,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ApiResponse.success(auditLogQueryService.search(request, page, size));
    }

    /**
     * 관리자가 최근 감사 로그 운영 지표를 조회합니다.
     *
     * @param from 조회 시작 시각, 없으면 최근 6시간 기준
     * @param to 조회 종료 시각, 없으면 현재 시각
     * @return 감사 로그 성공/실패율, 도메인별 건수, 비동기 작업 지표, 최근 실패 로그
     */
    @Operation(summary = "감사 로그 운영 지표 조회", description = "관리자가 최근 6시간 기준 감사 로그 운영 지표를 조회합니다.")
    @GetMapping("/summary")
    public ApiResponse<AuditLogSummaryResponse> summary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) {
        return ApiResponse.success(auditLogSummaryService.summarize(from, to));
    }
}
