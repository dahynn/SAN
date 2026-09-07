package com.san.api.domain.scrap.controller;

import com.san.api.domain.scrap.dto.request.ScrapCreateRequest;
import com.san.api.domain.scrap.dto.response.ScrapResponse;
import com.san.api.domain.scrap.service.ScrapService;
import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.CommonErrorCode;
import com.san.api.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** 수집 원본 API Controller */
@Tag(name = "Scrap", description = "수집 원본 API")
@RestController
@RequestMapping("/scraps")
@RequiredArgsConstructor
public class ScrapController {

    private final ScrapService scrapService;

    /**
     * 수집 원본 저장 및 지식카드 분석 작업 등록
     *
     * @param authentication 인증 정보
     * @param request 수집 원본 저장 요청
     * @return 저장된 수집 원본과 분석 작업 또는 지식카드 ID 응답
     */
    @Operation(summary = "수집 원본 저장 및 지식카드 분석 작업 등록", description = "전달받은 원본 데이터를 Scrap으로 저장하고 지식카드 분석 작업을 등록")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ScrapResponse> createScrap(
            Authentication authentication,
            @Valid @RequestBody ScrapCreateRequest request) {

        UUID userId = currentUserId(authentication);
        ScrapResponse response = scrapService.createScrap(userId, request);

        return ApiResponse.success(response);
    }

    /**
     * 인증 정보의 사용자 ID 추출
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
