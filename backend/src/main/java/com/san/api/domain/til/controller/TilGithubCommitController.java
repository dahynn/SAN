package com.san.api.domain.til.controller;

import com.san.api.domain.til.dto.response.TilGithubContributionResponse;
import com.san.api.domain.til.dto.response.TilGithubCommitJobResponse;
import com.san.api.domain.til.service.TilGithubContributionService;
import com.san.api.domain.til.service.TilGithubCommitService;
import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.CommonErrorCode;
import com.san.api.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

/** TIL GitHub 커밋 API Controller */
@Tag(name = "TIL GitHub Commit", description = "TIL GitHub 커밋 API")
@RestController
@RequestMapping("/til")
@RequiredArgsConstructor
public class TilGithubCommitController {

    private final TilGithubCommitService tilGithubCommitService;
    private final TilGithubContributionService tilGithubContributionService;

    /**
     * TIL GitHub 커밋 작업을 등록합니다.
     *
     * @param authentication 인증 정보
     * @param summaryId 커밋할 TIL ID
     * @return 등록된 TIL GitHub 커밋 작업 정보
     */
    @Operation(summary = "TIL GitHub 커밋 작업 등록", description = "생성된 TIL을 연결된 GitHub 저장소에 커밋하는 비동기 작업을 등록")
    @PostMapping("/{summaryId}/github-commit")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ApiResponse<TilGithubCommitJobResponse> commitTilToGithub(
            Authentication authentication,
            @PathVariable UUID summaryId) {

        TilGithubCommitService.RequestResult result = tilGithubCommitService.requestCommit(
                currentUserId(authentication),
                summaryId
        );

        return ApiResponse.success(TilGithubCommitJobResponse.from(result));
    }

    @Operation(summary = "TIL GitHub contribution 조회", description = "서비스에서 GitHub로 성공적으로 커밋한 TIL 기록을 잔디 UI용으로 조회합니다.")
    @GetMapping("/github-commits/contributions")
    public ApiResponse<TilGithubContributionResponse> getGithubContributions(
            Authentication authentication,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(required = false) Long githubRepositoryId) {

        TilGithubContributionResponse response = tilGithubContributionService.getContributions(
                currentUserId(authentication),
                from,
                to,
                githubRepositoryId
        );

        return ApiResponse.success(response);
    }

    private UUID currentUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED);
        }

        return UUID.fromString((String) authentication.getPrincipal());
    }
}
