package com.san.api.domain.til.controller;

import com.san.api.domain.til.dto.request.TilGenerateRequest;
import com.san.api.domain.til.dto.request.TilUpdateRequest;
import com.san.api.domain.til.dto.response.TilGenerationJobResponse;
import com.san.api.domain.til.dto.response.TilRecallCardsResponse;
import com.san.api.domain.til.dto.response.TilResponse;
import com.san.api.domain.til.dto.response.TilSourcesResponse;
import com.san.api.domain.til.service.TilService;
import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.CommonErrorCode;
import com.san.api.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** TIL API Controller */
@Tag(name = "TIL", description = "TIL API")
@RestController
@RequestMapping("/tils")
@RequiredArgsConstructor
public class TilController {

    private final TilService tilService;

    /**
     * TIL 생성 작업 등록
     *
     * @param authentication 인증 정보
     * @param request        TIL 생성 작업 등록 요청
     * @return 등록된 TIL 생성 작업 응답
     */
    @Operation(summary = "TIL 생성 작업 등록", description = "대상 날짜의 지식카드 원본을 기반으로 TIL을 생성하는 비동기 작업을 등록")
    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ApiResponse<TilGenerationJobResponse> generateTil(
            Authentication authentication,
            @Valid @RequestBody TilGenerateRequest request) {

        UUID userId = currentUserId(authentication);
        TilGenerationJobResponse response = tilService.requestGeneration(userId, request);

        return ApiResponse.success(response);
    }

    /**
     * TIL 삭제
     *
     * @param authentication 인증 정보
     * @param summaryId      TIL ID
     * @return 삭제 성공 응답
     */
    @Operation(summary = "TIL 삭제", description = "TIL을 삭제 처리")
    @DeleteMapping("/{summaryId}")
    public ApiResponse<Void> deleteTil(
            Authentication authentication,
            @PathVariable UUID summaryId) {

        UUID userId = currentUserId(authentication);
        tilService.deleteTil(summaryId, userId);

        return ApiResponse.success(null);
    }

    /**
     * 날짜 기준 TIL 조회
     *
     * @param authentication 인증 정보
     * @param date 조회 대상 날짜
     * @return 날짜 기준 TIL 조회 응답
     */
    @Operation(summary = "날짜 기준 TIL 조회", description = "프론트 달력에서 선택한 날짜의 TIL을 조회")
    @GetMapping
    public ApiResponse<List<TilResponse>> getTil(
            Authentication authentication,
            @RequestParam LocalDate date) {

        UUID userId = currentUserId(authentication);
        List<TilResponse> response = tilService.getTil(userId, date);

        return ApiResponse.success(response);
    }

    /**
     * TIL 제목과 내용 수정
     *
     * @param authentication 인증 정보
     * @param summaryId      TIL ID
     * @param request        TIL 수정 요청
     * @return 수정된 TIL 응답
     */
    @Operation(summary = "TIL 수정", description = "TIL 제목과 내용을 수정")
    @PatchMapping("/{summaryId}")
    public ApiResponse<TilResponse> updateTil(
            Authentication authentication,
            @PathVariable UUID summaryId,
            @Valid @RequestBody TilUpdateRequest request) {

        UUID userId = currentUserId(authentication);
        TilResponse response = tilService.updateTil(summaryId, userId, request);

        return ApiResponse.success(response);
    }

    /**
     * TIL 기반 리콜 카드 조회
     *
     * @param authentication 인증 정보
     * @param summaryId      TIL ID
     * @return 리콜 카드 목록
     */
    @Operation(summary = "TIL 리콜 카드 조회", description = "TIL 임베딩 기반으로 유사한 지식카드를 추천. 원본 카드는 제외되며 유사도 threshold 이상인 카드를 전체 반환")
    @GetMapping("/{summaryId}/recall-cards")
    public ApiResponse<TilRecallCardsResponse> getRecallCards(
            Authentication authentication,
            @PathVariable UUID summaryId) {

        UUID userId = currentUserId(authentication);
        TilRecallCardsResponse response = tilService.getRecallCards(summaryId, userId);

        return ApiResponse.success(response);
    }

    /**
     * TIL 생성 원본 조회
     *
     * @param authentication 인증 정보
     * @param summaryId      TIL ID
     * @return TIL 생성 원본 목록
     */
    @Operation(summary = "TIL 생성 원본 조회", description = "TIL 생성에 사용된 지식카드 원본 목록을 조회")
    @GetMapping("/{summaryId}/source")
    public ApiResponse<TilSourcesResponse> getSources(
            Authentication authentication,
            @PathVariable UUID summaryId) {

        UUID userId = currentUserId(authentication);
        TilSourcesResponse response = tilService.getSources(summaryId, userId);

        return ApiResponse.success(response);
    }

    private UUID currentUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED);
        }

        return UUID.fromString((String) authentication.getPrincipal());
    }
}
