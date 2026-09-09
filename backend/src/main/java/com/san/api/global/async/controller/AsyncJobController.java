package com.san.api.global.async.controller;

import com.san.api.global.async.dto.response.AsyncJobStatusResponse;
import com.san.api.global.async.service.AsyncJobManager;
import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.CommonErrorCode;
import com.san.api.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * 비동기 잡 상태 조회 API.
 *
 * 프론트엔드 Short Polling 용도로 사용된다.
 */
@Tag(name = "AsyncJob", description = "비동기 작업 상태 조회 API")
@RestController
@RequestMapping("/async-jobs")
@RequiredArgsConstructor
public class AsyncJobController {

    private final AsyncJobManager asyncJobManager;

    @Operation(summary = "비동기 작업 상태 조회")
    @GetMapping("/{jobId}")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<AsyncJobStatusResponse> getJobStatus(Authentication authentication, @PathVariable UUID jobId) {
        return ApiResponse.success(AsyncJobStatusResponse.from(asyncJobManager.getJobForUser(jobId, currentUserId(authentication))));
    }

    private UUID currentUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED);
        }
        return UUID.fromString((String) authentication.getPrincipal());
    }
}
