package com.san.api.domain.github.controller;

import com.san.api.domain.github.dto.response.GithubStarRecommendationCollectResponse;
import com.san.api.domain.github.dto.response.GithubStarRecommendationJobResponse;
import com.san.api.domain.github.dto.response.GithubStarRecommendationListResponse;
import com.san.api.domain.github.service.GithubStarRecommendationCollectService;
import com.san.api.domain.github.service.GithubStarRecommendationJobService;
import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.CommonErrorCode;
import com.san.api.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** GitHub Star 추천 API Controller */
@Tag(name = "GitHub Star Recommendation", description = "GitHub Star 추천 API")
@RestController
@RequestMapping("/github/star-recommendations")
@RequiredArgsConstructor
public class GithubStarRecommendationController {

    private final GithubStarRecommendationJobService githubStarRecommendationJobService;
    private final GithubStarRecommendationCollectService githubStarRecommendationCollectService;

    /**
     * GitHub Star 추천 작업 요청
     *
     * @param authentication 인증 사용자 정보
     * @return GitHub Star 추천 작업 응답
     */
    @Operation(
            summary = "GitHub Star 추천 작업 요청",
            description = "최초 요청이면 GitHub Star 추천 작업을 등록하고, 이미 추천 후보가 있으면 기존 추천 후보를 반환"
    )
    @PostMapping
    public ResponseEntity<ApiResponse<GithubStarRecommendationJobResponse>> requestRecommendation(
            Authentication authentication) {

        UUID userId = currentUserId(authentication);
        GithubStarRecommendationJobResponse response = githubStarRecommendationJobService.requestRecommendation(userId);

        HttpStatus status = response.alreadyRecommended() ? HttpStatus.OK : HttpStatus.ACCEPTED;
        return ResponseEntity.status(status).body(ApiResponse.success(response));
    }

    /**
     * 로그인 사용자 기준 GitHub Star 추천 후보 목록 조회
     *
     * @param authentication 인증 사용자 정보
     * @return GitHub Star 추천 후보 목록 응답
     */
    @Operation(summary = "GitHub Star 추천 후보 목록 조회", description = "로그인 사용자의 GitHub Star 추천 후보 목록을 최신순으로 조회")
    @GetMapping
    public ApiResponse<GithubStarRecommendationListResponse> getRecommendations(Authentication authentication) {
        UUID userId = currentUserId(authentication);
        GithubStarRecommendationListResponse response = githubStarRecommendationJobService.getRecommendations(userId);

        return ApiResponse.success(response);
    }

    /**
     * GitHub Star 추천 후보 수집
     *
     * @param authentication 인증 사용자 정보
     * @param recommendationId 추천 후보 ID
     * @return 수집된 원본과 지식카드 ID 응답
     */
    @Operation(summary = "GitHub Star 추천 후보 수집", description = "추천 후보의 저장된 분석 결과를 사용해 원본과 지식카드를 생성")
    @PostMapping("/{recommendationId}/collect")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<GithubStarRecommendationCollectResponse> collectRecommendation(
            Authentication authentication,
            @PathVariable UUID recommendationId) {

        UUID userId = currentUserId(authentication);
        GithubStarRecommendationCollectResponse response =
                githubStarRecommendationCollectService.collect(userId, recommendationId);

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
