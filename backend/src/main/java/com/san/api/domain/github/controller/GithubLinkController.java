package com.san.api.domain.github.controller;

import com.san.api.domain.github.dto.response.GithubAuthorizationUrlResponse;
import com.san.api.domain.github.dto.response.GithubLinkStatusResponse;
import com.san.api.domain.github.service.GithubLinkService;
import com.san.api.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.util.UUID;

/** 로그인된 서비스 계정의 GitHub 계정 연동 요청을 처리하는 컨트롤러. */
@Tag(name = "GitHub Link", description = "GitHub 계정 연동 API")
@RestController
@RequestMapping("/github/link")
@RequiredArgsConstructor
public class GithubLinkController {

    private final GithubLinkService githubLinkService;

    @Operation(
            summary = "GitHub 계정 연동 시작",
            description = "현재 로그인한 서비스 계정에 GitHub 계정을 연결하기 위해 GitHub authorize 페이지로 리다이렉트합니다."
    )
    @GetMapping("/authorize")
    @ResponseStatus(HttpStatus.FOUND)
    public RedirectView authorize(Authentication authentication) {
        return new RedirectView(githubLinkService.createLinkAuthorizationRedirectUrl(currentUserId(authentication)));
    }

    @Operation(
            summary = "GitHub 계정 연동 URL 조회",
            description = "Authorization 헤더로 인증한 뒤 프론트/익스텐션이 직접 이동할 GitHub authorize URL을 반환합니다."
    )
    @GetMapping("/authorize-url")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<GithubAuthorizationUrlResponse> authorizeUrl(Authentication authentication) {
        return ApiResponse.success(
                new GithubAuthorizationUrlResponse(
                        githubLinkService.createLinkAuthorizationRedirectUrl(currentUserId(authentication))
                )
        );
    }

    @Operation(
            summary = "GitHub 연결 status",
            description = "현재 서비스 계정의 GitHub 계정 연결 여부와 선택된 레포지토리 정보를 조회합니다."
    )
    @GetMapping("/status")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<GithubLinkStatusResponse> status(Authentication authentication) {
        return ApiResponse.success(githubLinkService.getLinkStatus(currentUserId(authentication)));
    }

    @Operation(
            summary = "GitHub 계정 연동 해제",
            description = "현재 로그인한 서비스 계정의 GitHub 계정과 연결된 레포지토리 정보를 삭제합니다."
    )
    @DeleteMapping
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Void> unlink(Authentication authentication) {
        githubLinkService.unlinkGithubAccount(currentUserId(authentication));
        return ApiResponse.success();
    }

    private UUID currentUserId(Authentication authentication) {
        return UUID.fromString((String) authentication.getPrincipal());
    }
}
