package com.san.api.global.external.s3.controller;

import com.san.api.global.external.s3.dto.request.S3PresignedUrlRequest;
import com.san.api.global.external.s3.dto.response.S3PresignedUrlResponse;
import com.san.api.global.external.s3.service.S3PresignedUrlService;
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

/** S3 Presigned URL API Controller */
@Tag(name = "S3", description = "S3 파일 업로드 API")
@RestController
@RequestMapping("/s3")
@RequiredArgsConstructor
public class S3PresignedUrlController {

    private final S3PresignedUrlService s3PresignedUrlService;

    /**
     * S3 직접 업로드를 위한 Presigned URL 발급
     *
     * @param request Presigned URL 발급 요청
     * @return S3 업로드 URL과 object key
     */
    @Operation(summary = "S3 Presigned URL 발급", description = "프론트엔드 S3 직접 업로드용 Presigned URL 발급")
    @PostMapping("/presigned-url")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<S3PresignedUrlResponse> createPresignedUrl(
            Authentication authentication,
            @Valid @RequestBody S3PresignedUrlRequest request) {

        UUID userId = currentUserId(authentication);
        S3PresignedUrlResponse response = s3PresignedUrlService.createPresignedUrl(userId, request);

        return ApiResponse.success(response);
    }

    private UUID currentUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED);
        }

        return UUID.fromString((String) authentication.getPrincipal());
    }
}
