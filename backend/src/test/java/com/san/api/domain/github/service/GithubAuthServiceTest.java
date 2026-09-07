package com.san.api.domain.github.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.san.api.domain.auth.dto.response.TokenResponse;
import com.san.api.domain.auth.entity.ClientType;
import com.san.api.domain.auth.service.TokenIssueService;
import com.san.api.domain.github.dto.request.GithubLoginRequest;
import com.san.api.domain.github.entity.GithubAccount;
import com.san.api.domain.github.repository.GithubAccountRepository;
import com.san.api.domain.user.entity.AuthProvider;
import com.san.api.domain.user.entity.User;
import com.san.api.domain.user.entity.UserRole;
import com.san.api.domain.user.repository.UserRepository;
import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.AuthErrorCode;
import com.san.api.global.external.github.client.GithubApiClient;
import com.san.api.global.external.github.dto.response.GithubAccessTokenResponse;
import com.san.api.global.external.github.dto.response.GithubUserProfileResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/** GitHub OAuth 로그인 callback과 연결 계정 로그인 처리를 검증하는 테스트. */
@ExtendWith(MockitoExtension.class)
class GithubAuthServiceTest {

    @Mock
    private GithubApiClient githubApiClient;

    @Mock
    private UserRepository userRepository;

    @Mock
    private GithubAccountRepository githubAccountRepository;

    @Mock
    private TokenIssueService tokenIssueService;

    @Mock
    private GithubLinkService githubLinkService;

    @Mock
    private StringRedisTemplate redisTemplate;

    private GithubAuthService githubAuthService;

    @BeforeEach
    void setUp() {
        githubAuthService = new GithubAuthService(
                githubApiClient,
                userRepository,
                githubAccountRepository,
                tokenIssueService,
                githubLinkService,
                redisTemplate,
                new ObjectMapper()
        );
        ReflectionTestUtils.setField(githubAuthService, "successRedirectUri", "http://localhost/success");
        ReflectionTestUtils.setField(githubAuthService, "failureRedirectUri", "http://localhost/failure");
    }

    @Test
    void loginUsesUserAlreadyLinkedWithGithubAccount() {
        User user = User.builder()
                .username("localuser")
                .passwordHash("password")
                .provider(AuthProvider.LOCAL)
                .build();
        GithubAccount githubAccount = new GithubAccount(user, "1", "octocat", "encrypted-token");
        GithubAccessTokenResponse githubToken = new GithubAccessTokenResponse("github-token", "bearer", "repo");
        GithubUserProfileResponse profile = new GithubUserProfileResponse(1L, "octocat");
        TokenResponse tokenResponse = TokenResponse.of("access-token", "refresh-token", 1800, "session-id");

        when(githubApiClient.requestAccessToken("code")).thenReturn(githubToken);
        when(githubApiClient.findUserProfile("github-token")).thenReturn(profile);
        when(githubAccountRepository.findByGithubUserId("1")).thenReturn(Optional.of(githubAccount));
        when(tokenIssueService.issueTokenPair(user.getUserId().toString(), ClientType.DASHBOARD, UserRole.USER))
                .thenReturn(tokenResponse);

        TokenResponse result = githubAuthService.login(new GithubLoginRequest("code", ClientType.DASHBOARD));

        assertThat(result).isEqualTo(tokenResponse);
        verify(githubLinkService).saveGithubAccount(user, profile, "github-token");
    }

    @Test
    void handleCallbackPreservesCurrentUserAlreadyLinkedErrorCode() {
        UUID userId = UUID.randomUUID();
        when(githubLinkService.consumeLinkUserId("state")).thenReturn(Optional.of(userId));
        doThrow(new BusinessException(AuthErrorCode.GITHUB_ACCOUNT_ALREADY_LINKED_TO_CURRENT_USER))
                .when(githubLinkService)
                .linkGithubAccount(userId, "code");

        String redirectUrl = githubAuthService.handleCallback("code", "state");

        assertThat(redirectUrl).isEqualTo("http://localhost/failure?error=A206");
    }

    @Test
    void handleCallbackPreservesOtherUserAlreadyLinkedErrorCode() {
        UUID userId = UUID.randomUUID();
        when(githubLinkService.consumeLinkUserId("state")).thenReturn(Optional.of(userId));
        doThrow(new BusinessException(AuthErrorCode.GITHUB_ACCOUNT_ALREADY_LINKED))
                .when(githubLinkService)
                .linkGithubAccount(userId, "code");

        String redirectUrl = githubAuthService.handleCallback("code", "state");

        assertThat(redirectUrl).isEqualTo("http://localhost/failure?error=A204");
    }
}
