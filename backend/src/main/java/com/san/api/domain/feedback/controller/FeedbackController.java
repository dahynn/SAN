package com.san.api.domain.feedback.controller;

import com.san.api.domain.auth.entity.ClientType;
import com.san.api.domain.feedback.dto.request.FeedbackCreateRequest;
import com.san.api.domain.feedback.dto.response.FeedbackCreateResponse;
import com.san.api.domain.feedback.service.FeedbackService;
import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.CommonErrorCode;
import com.san.api.global.response.ApiResponse;
import com.san.api.global.security.jwt.JwtSessionClaims;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/** 서비스 피드백 등록 API Controller. */
@Tag(name = "Feedback", description = "서비스 피드백 API")
@RestController
@RequestMapping("/feedback")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;

    /**
     * 로그인한 사용자의 서비스 피드백을 등록합니다.
     *
     * @param authentication 인증 정보
     * @param request 피드백 등록 요청
     * @return 등록된 피드백 ID
     */
    @Operation(summary = "서비스 피드백 등록")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<FeedbackCreateResponse> createFeedback(
            Authentication authentication,
            @Valid @RequestBody FeedbackCreateRequest request) {
        UUID feedbackId = feedbackService.createFeedback(
                currentUserId(authentication),
                currentClientType(authentication),
                request
        );
        return ApiResponse.success(new FeedbackCreateResponse(feedbackId));
    }

    /** 인증 details에 저장된 세션 클레임에서 클라이언트 유형을 추출합니다. */
    private ClientType currentClientType(Authentication authentication) {
        if (authentication == null || !(authentication.getDetails() instanceof JwtSessionClaims sessionClaims)) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED);
        }
        return sessionClaims.clientType();
    }

    /** 인증 정보에서 로그인 사용자 ID를 추출합니다. */
    private UUID currentUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED);
        }

        return UUID.fromString((String) authentication.getPrincipal());
    }
}
