package com.san.api.domain.recall.controller;

import com.san.api.domain.recall.dto.request.RecallQuizGenerateRequest;
import com.san.api.domain.recall.dto.request.RecallQuizSubmitRequest;
import com.san.api.domain.recall.dto.response.RecallQuizGenerationJobResponse;
import com.san.api.domain.recall.dto.response.RecallQuizListResponse;
import com.san.api.domain.recall.dto.response.RecallQuizSubmitResponse;
import com.san.api.domain.recall.entity.RecallQuizType;
import com.san.api.domain.recall.service.RecallQuizGenerationService;
import com.san.api.domain.recall.service.RecallQuizSubmissionService;
import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.CommonErrorCode;
import com.san.api.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

/** 리콜 API Controller */
@Tag(name = "Recall", description = "리콜 API")
@RestController
@RequestMapping("/recall")
@RequiredArgsConstructor
public class RecallController {

    private final RecallQuizGenerationService recallQuizGenerationService;
    private final RecallQuizSubmissionService recallQuizSubmissionService;

    /**
     * 날짜 기반 리콜 퀴즈 생성 작업 등록
     *
     * @param authentication 인증 정보
     * @param request 리콜 퀴즈 생성 요청
     * @return 등록된 리콜 퀴즈 생성 작업 응답
     */
    @Operation(summary = "날짜 기반 리콜 퀴즈 생성 작업 등록", description = "대상 날짜의 TIL 원본 기반 리콜 퀴즈 생성을 비동기 작업으로 등록")
    @PostMapping("/quizzes")
    public ApiResponse<RecallQuizGenerationJobResponse> generate(
            Authentication authentication,
            @Valid @RequestBody RecallQuizGenerateRequest request) {

        UUID userId = currentUserId(authentication);
        RecallQuizGenerationJobResponse response = recallQuizGenerationService.requestGeneration(userId, request);

        return ApiResponse.success(response);
    }

    /**
     * 날짜와 유형 기준 리콜 퀴즈 조회
     *
     * @param authentication 인증 정보
     * @param targetDate 조회 대상 날짜
     * @param quizType 리콜 퀴즈 유형
     * @return 리콜 퀴즈 목록 응답
     */
    @Operation(summary = "날짜와 유형 기준 리콜 퀴즈 조회", description = "대상 날짜와 퀴즈 유형에 해당하는 리콜 퀴즈 목록 조회")
    @GetMapping("/quizzes")
    public ApiResponse<RecallQuizListResponse> getQuizzes(
            Authentication authentication,
            @RequestParam LocalDate targetDate,
            @RequestParam RecallQuizType quizType) {

        UUID userId = currentUserId(authentication);
        RecallQuizListResponse response = recallQuizGenerationService.getQuizzes(userId, targetDate, quizType);

        return ApiResponse.success(response);
    }

    /**
     * 리콜 퀴즈 정답 제출
     *
     * @param authentication 인증 정보
     * @param quizId 리콜 퀴즈 ID
     * @param request 리콜 퀴즈 정답 제출 요청
     * @return 리콜 퀴즈 정답 제출 응답
     */
    @Operation(summary = "리콜 퀴즈 정답 제출", description = "리콜 퀴즈에 사용자가 입력한 답변 제출")
    @PostMapping("/quizzes/{quizId}/submissions")
    public ApiResponse<RecallQuizSubmitResponse> submit(
            Authentication authentication,
            @PathVariable UUID quizId,
            @Valid @RequestBody RecallQuizSubmitRequest request) {

        UUID userId = currentUserId(authentication);
        RecallQuizSubmitResponse response = recallQuizSubmissionService.submit(userId, quizId, request);

        return ApiResponse.success(response);
    }

    /** 인증 정보에서 사용자 ID 추출 */
    private UUID currentUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED);
        }

        return UUID.fromString((String) authentication.getPrincipal());
    }
}
