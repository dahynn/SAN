package com.san.api.domain.archive.controller;

import com.san.api.domain.archive.dto.response.ArchiveCardTagRelationResponse;
import com.san.api.domain.archive.dto.response.ArchiveCategoryCardListResponse;
import com.san.api.domain.archive.dto.response.ArchiveCategoryListResponse;
import com.san.api.domain.archive.service.ArchiveService;
import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.CommonErrorCode;
import com.san.api.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** 아카이브 API Controller */
@Tag(name = "Archive", description = "아카이브 API")
@RestController
@RequestMapping("/archives")
@RequiredArgsConstructor
public class ArchiveController {

    private final ArchiveService archiveService;

    /**
     * 아카이브 카테고리 목록 조회
     *
     * @param authentication 인증 정보
     * @return 카테고리별 지식카드 개수 응답
     */
    @Operation(summary = "아카이브 카테고리 조회", description = "지식나무 초기 화면에 사용할 카테고리 목록과 카테고리별 지식카드 개수를 조회")
    @GetMapping("/categories")
    public ApiResponse<ArchiveCategoryListResponse> getCategories(Authentication authentication) {
        UUID userId = currentUserId(authentication);
        ArchiveCategoryListResponse response = archiveService.getCategories(userId);

        return ApiResponse.success(response);
    }

    /**
     * 아카이브 카테고리별 지식카드 목록 조회
     *
     * @param authentication 인증 정보
     * @param categoryId 카테고리 ID
     * @return 카테고리별 지식카드 목록 응답
     */
    @Operation(summary = "아카이브 카테고리별 지식카드 조회", description = "선택한 카테고리에 속한 지식카드 목록을 최신순으로 조회")
    @GetMapping("/categories/{categoryId}/cards")
    public ApiResponse<ArchiveCategoryCardListResponse> getCategoryCards(
            Authentication authentication,
            @PathVariable UUID categoryId) {

        UUID userId = currentUserId(authentication);
        ArchiveCategoryCardListResponse response = archiveService.getCategoryCards(userId, categoryId);

        return ApiResponse.success(response);
    }

    /**
     * 아카이브 카드 태그 연관도 조회
     *
     * @param authentication 인증 정보
     * @param cardId 선택 지식카드 ID
     * @return 태그가 겹치는 지식카드 목록 응답
     */
    @Operation(summary = "아카이브 카드 태그 연관도 조회", description = "선택한 지식카드와 태그가 겹치는 로그인 사용자의 전체 지식카드를 조회")
    @GetMapping("/cards/{cardId}/tag-relations")
    public ApiResponse<ArchiveCardTagRelationResponse> getCardTagRelations(
            Authentication authentication,
            @PathVariable UUID cardId) {

        UUID userId = currentUserId(authentication);
        ArchiveCardTagRelationResponse response = archiveService.getCardTagRelations(userId, cardId);

        return ApiResponse.success(response);
    }

    private UUID currentUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED);
        }

        return UUID.fromString((String) authentication.getPrincipal());
    }
}
