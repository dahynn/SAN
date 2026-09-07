package com.san.api.domain.github.controller;

import com.san.api.domain.github.dto.request.GithubRepositoryConnectRequest;
import com.san.api.domain.github.dto.response.GithubRepositoryResponse;
import com.san.api.domain.github.service.GithubRepositoryService;
import com.san.api.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** GitHub 레포지토리 조회와 연결 요청을 처리하는 컨트롤러. */
@Tag(name = "GitHub Repository", description = "GitHub 레포지토리 연동 API")
@RestController
@RequestMapping("/github/repositories")
@RequiredArgsConstructor
public class GithubRepositoryController {

    private final GithubRepositoryService githubRepositoryService;

    @Operation(
            summary = "GitHub 레포 목록 조회",
            description = "저장된 GitHub access token으로 사용자가 접근 가능한 레포 목록을 조회합니다."
    )
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<List<GithubRepositoryResponse>> repositories(Authentication authentication) {
        return ApiResponse.success(githubRepositoryService.findRepositories(currentUserId(authentication)));
    }

    @Operation(
            summary = "GitHub 레포 연결",
            description = "사용자가 선택한 GitHub 레포 ID를 검증한 뒤 서비스 계정에 연결합니다."
    )
    @PostMapping("/connect")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<GithubRepositoryResponse> connectRepository(
            Authentication authentication,
            @Valid @RequestBody GithubRepositoryConnectRequest request) {
        return ApiResponse.success(githubRepositoryService.connectRepository(currentUserId(authentication), request));
    }

    @Operation(
            summary = "GitHub 레포 연결 해제",
            description = "현재 서비스 계정에 연결된 GitHub 레포를 연결 해제합니다."
    )
    @DeleteMapping("/{repositoryId}")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Void> disconnectRepository(
            Authentication authentication,
            @PathVariable Long repositoryId) {
        githubRepositoryService.disconnectRepository(currentUserId(authentication), repositoryId);
        return ApiResponse.success();
    }

    private UUID currentUserId(Authentication authentication) {
        return UUID.fromString((String) authentication.getPrincipal());
    }
}
