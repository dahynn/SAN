package com.san.api.domain.statistics.controller;

import com.san.api.domain.statistics.dto.response.StatisticsOverviewResponse;
import com.san.api.domain.statistics.service.StatisticsService;
import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.CommonErrorCode;
import com.san.api.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** 사용자 통계 API Controller */
@Tag(name = "통계", description = "사용자 통계 API")
@RestController
@RequestMapping("/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;

    /**
     * 대시보드 숫자 카드에 표시할 사용자 통계 요약을 조회합니다.
     *
     * @param authentication 인증 정보
     * @return 통계 요약 응답
     */
    @Operation(summary = "통계 요약 조회", description = "대시보드 숫자 카드에 사용할 지식 카드와 TIL 개수를 조회")
    @GetMapping("/overview")
    public ApiResponse<StatisticsOverviewResponse> getOverview(Authentication authentication) {
        return ApiResponse.success(statisticsService.getOverview(currentUserId(authentication)));
    }

    /**
     * 인증 정보에서 사용자 ID를 추출합니다.
     *
     * @param authentication 인증 정보
     * @return 로그인 사용자 ID
     */
    private UUID currentUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED);
        }

        return UUID.fromString((String) authentication.getPrincipal());
    }
}
