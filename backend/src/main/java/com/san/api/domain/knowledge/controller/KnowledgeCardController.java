package com.san.api.domain.knowledge.controller;

import com.san.api.domain.knowledge.dto.request.KnowledgeCardCreateRequest;
import com.san.api.domain.knowledge.dto.request.RefinedContentUpdateRequest;
import com.san.api.domain.knowledge.dto.response.KnowledgeCardAnalysisJobResponse;
import com.san.api.domain.knowledge.dto.response.KnowledgeCardDetailResponse;
import com.san.api.domain.knowledge.dto.response.KnowledgeCardIdResponse;
import com.san.api.domain.knowledge.dto.response.KnowledgeCardListResponse;
import com.san.api.domain.knowledge.dto.response.KnowledgeCardSimilarCardsResponse;
import com.san.api.domain.knowledge.service.KnowledgeCardService;
import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.CommonErrorCode;
import com.san.api.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** 지식카드 API Controller */
@Tag(name = "KnowledgeCard", description = "지식카드 API")
@RestController
@RequestMapping("/cards")
@RequiredArgsConstructor
public class KnowledgeCardController {

    private final KnowledgeCardService knowledgeCardService;

    /**
     * 저장된 수집 원본 기반 지식카드 AI 분석 작업 등록
     *
     * @param authentication 인증 정보
     * @param request 지식카드 AI 분석 요청
     * @return 등록된 비동기 작업 응답
     */
    @Operation(summary = "지식카드 AI 분석 작업 등록", description = "저장된 수집 원본을 지식카드로 분석하는 비동기 작업을 등록")
    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ApiResponse<KnowledgeCardAnalysisJobResponse> createCard(
            Authentication authentication,
            @Valid @RequestBody KnowledgeCardCreateRequest request) {

        UUID userId = currentUserId(authentication);
        KnowledgeCardAnalysisJobResponse response = knowledgeCardService.createCard(userId, request);

        return ApiResponse.success(response);
    }

    /**
     * 수집 원본 기준 생성된 지식카드 ID 조회
     *
     * @param authentication 인증 정보
     * @param scrapId 수집 원본 ID
     * @return 생성된 지식카드 ID 응답
     */
    @Operation(summary = "수집 원본 기준 지식카드 ID 조회", description = "수집 원본 ID를 기준으로 생성된 지식카드 ID를 조회")
    @GetMapping("/{scrapId}")
    public ApiResponse<KnowledgeCardIdResponse> getCardIdByScrap(
            Authentication authentication,
            @PathVariable UUID scrapId) {

        UUID userId = currentUserId(authentication);
        KnowledgeCardIdResponse response = knowledgeCardService.getCardIdByScrap(userId, scrapId);

        return ApiResponse.success(response);
    }

    /**
     * 지식카드 삭제
     *
     * @param authentication 인증 정보
     * @param cardId 지식카드 ID
     * @return 삭제 처리 결과
     */
    @Operation(summary = "지식카드 삭제", description = "지식카드 상세 화면에서 지식카드를 삭제")
    @DeleteMapping("/{cardId}")
    public ApiResponse<Void> deleteCard(
            Authentication authentication,
            @PathVariable UUID cardId) {

        UUID userId = currentUserId(authentication);
        knowledgeCardService.deleteCard(userId, cardId);

        return ApiResponse.success(null);
    }

    /**
     * 지식카드 상세 조회
     *
     * @param authentication 인증 정보
     * @param cardId 지식카드 ID
     * @return 지식카드 상세 조회 응답
     */
    @Operation(summary = "지식카드 상세 조회", description = "지식카드 ID를 기반으로 원본, 정제 내용, 요약, 태그, 카테고리를 조회")
    @GetMapping("/{cardId}/detail")
    public ApiResponse<KnowledgeCardDetailResponse> getCardDetail(
            Authentication authentication,
            @PathVariable UUID cardId) {

        UUID userId = currentUserId(authentication);
        KnowledgeCardDetailResponse response = knowledgeCardService.getCardDetail(userId, cardId);

        return ApiResponse.success(response);
    }

    /**
     * 지식카드 상세보기에서 정제원본 수정
     *
     * @param authentication 인증 정보
     * @param cardId 지식카드 ID
     * @param request 정제원본 수정 요청
     * @return 수정된 지식카드 상세 조회 응답
     */
    @Operation(summary = "지식카드 정제원본 수정", description = "지식카드 상세보기에서 정제원본 내용을 수정")
    @PatchMapping("/{cardId}/refined-content")
    public ApiResponse<KnowledgeCardDetailResponse> updateRefinedContent(
            Authentication authentication,
            @PathVariable UUID cardId,
            @Valid @RequestBody RefinedContentUpdateRequest request) {

        UUID userId = currentUserId(authentication);
        KnowledgeCardDetailResponse response = knowledgeCardService.updateRefinedContent(userId, cardId, request);

        return ApiResponse.success(response);
    }

    /**
     * 지식카드 기준 유사 카드 조회
     *
     * @param authentication 인증 정보
     * @param cardId 지식카드 ID
     * @return 유사 지식카드 목록 응답
     */
    @Operation(summary = "지식카드 기준 유사 카드 조회", description = "지식카드 ID를 기준으로 유사한 지식카드 3개를 조회")
    @GetMapping("/{cardId}/similar-cards")
    public ApiResponse<KnowledgeCardSimilarCardsResponse> getSimilarCardsByCard(
            Authentication authentication,
            @PathVariable UUID cardId) {

        UUID userId = currentUserId(authentication);
        KnowledgeCardSimilarCardsResponse response = knowledgeCardService.getSimilarCardsByCard(userId, cardId);

        return ApiResponse.success(response);
    }

    /**
     * 로그인 사용자 기준 지식카드 목록 조회
     *
     * @param authentication 인증 정보
     * @return 지식카드 목록 응답
     */
    @Operation(summary = "지식카드 목록 조회", description = "로그인 사용자의 지식카드 목록을 최신순으로 조회")
    @GetMapping
    public ApiResponse<KnowledgeCardListResponse> getCards(Authentication authentication) {
        UUID userId = currentUserId(authentication);
        KnowledgeCardListResponse response = knowledgeCardService.getCards(userId);

        return ApiResponse.success(response);
    }

    /**
     * 인증 정보에서 사용자 ID 추출
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
