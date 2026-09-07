package com.san.api.domain.github.service;

import com.san.api.domain.github.entity.GithubAccount;
import com.san.api.domain.github.repository.GithubAccountRepository;
import com.san.api.domain.github.repository.GithubRepositoryConnectionRepository;
import com.san.api.domain.user.entity.AuthProvider;
import com.san.api.domain.user.entity.User;
import com.san.api.domain.user.repository.UserRepository;
import com.san.api.global.audit.entity.AuditEventType;
import com.san.api.global.audit.entity.AuditTargetType;
import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.AuthErrorCode;
import com.san.api.global.external.github.client.GithubApiClient;
import com.san.api.global.external.github.dto.response.GithubAccessTokenResponse;
import com.san.api.global.external.github.dto.response.GithubUserProfileResponse;
import com.san.api.global.security.crypto.AesGcmStringEncryptor;
import com.san.api.global.security.redis.AuthRedisKeyPrefix;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/** GitHub 계정 연동 state 관리와 계정 연결 저장을 검증하는 테스트. */
@ExtendWith(MockitoExtension.class)
class GithubLinkServiceTest {

    @Mock
    private GithubApiClient githubApiClient;

    @Mock
    private UserRepository userRepository;

    @Mock
    private GithubAccountRepository githubAccountRepository;

    @Mock
    private GithubRepositoryConnectionRepository connectionRepository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private AesGcmStringEncryptor encryptor;

    @Mock
    private GithubAuditService githubAuditService;

    private GithubLinkService githubLinkService;

    @BeforeEach
    void setUp() {
        githubLinkService = new GithubLinkService(
                githubApiClient,
                userRepository,
                githubAccountRepository,
                connectionRepository,
                redisTemplate,
                encryptor,
                githubAuditService
        );
    }

    @Test
    void createLinkAuthorizationRedirectUrlStoresUserIdByState() {
        UUID userId = UUID.randomUUID();
        when(userRepository.existsById(userId)).thenReturn(true);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(githubApiClient.createAuthorizationUrl(anyString())).thenReturn("https://github.com/oauth");

        String redirectUrl = githubLinkService.createLinkAuthorizationRedirectUrl(userId);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(keyCaptor.capture(), valueCaptor.capture(), any(Duration.class));
        assertThat(keyCaptor.getValue()).startsWith(AuthRedisKeyPrefix.GITHUB_LINK_STATE);
        assertThat(valueCaptor.getValue()).isEqualTo(userId.toString());
        assertThat(redirectUrl).isEqualTo("https://github.com/oauth");
    }

    @Test
    void consumeLinkUserIdReturnsUserIdAndDeletesState() {
        UUID userId = UUID.randomUUID();
        String state = "state-token";
        String key = AuthRedisKeyPrefix.GITHUB_LINK_STATE + state;
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(key)).thenReturn(userId.toString());

        Optional<UUID> result = githubLinkService.consumeLinkUserId(state);

        assertThat(result).contains(userId);
        verify(redisTemplate).delete(key);
    }

    @Test
    void getLinkStatusWithoutGithubAccountReturnsNotLinked() {
        UUID userId = UUID.randomUUID();
        when(githubAccountRepository.findByUser_UserId(userId)).thenReturn(Optional.empty());

        var result = githubLinkService.getLinkStatus(userId);

        assertThat(result.linked()).isFalse();
        assertThat(result.githubUsername()).isNull();
        assertThat(result.repositoryConnected()).isFalse();
        assertThat(result.connectedRepository()).isNull();
    }

    @Test
    void getLinkStatusWithGithubAccountAndRepositoryReturnsConnectedRepository() {
        User user = User.builder()
                .username("localuser")
                .passwordHash("password")
                .provider(AuthProvider.LOCAL)
                .build();
        UUID userId = user.getUserId();
        GithubAccount githubAccount = new GithubAccount(user, "1", "octocat", "encrypted-token");
        var repositoryConnection = new com.san.api.domain.github.entity.GithubRepositoryConnection(
                user,
                new com.san.api.global.external.github.dto.response.ExternalGithubRepositoryResponse(
                        100L,
                        "til",
                        "octocat/til",
                        false,
                        "main",
                        "https://github.com/octocat/til"
                )
        );
        when(githubAccountRepository.findByUser_UserId(userId)).thenReturn(Optional.of(githubAccount));
        when(connectionRepository.findByUser_UserId(userId)).thenReturn(Optional.of(repositoryConnection));

        var result = githubLinkService.getLinkStatus(userId);

        assertThat(result.linked()).isTrue();
        assertThat(result.githubUsername()).isEqualTo("octocat");
        assertThat(result.repositoryConnected()).isTrue();
        assertThat(result.connectedRepository().githubRepositoryId()).isEqualTo(100L);
        assertThat(result.connectedRepository().fullName()).isEqualTo("octocat/til");
    }

    @Test
    void linkGithubAccountRecordsAuditSuccess() {
        User user = User.builder()
                .username("localuser")
                .passwordHash("password")
                .provider(AuthProvider.LOCAL)
                .build();
        UUID userId = user.getUserId();
        GithubUserProfileResponse profile = new GithubUserProfileResponse(1L, "octocat");
        GithubAccount githubAccount = new GithubAccount(user, "1", "octocat", "encrypted-token");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(githubAccountRepository.findByUser_UserId(userId)).thenReturn(Optional.empty());
        when(githubApiClient.requestAccessToken("code"))
                .thenReturn(new GithubAccessTokenResponse("github-token", "bearer", "repo"));
        when(githubApiClient.findUserProfile("github-token")).thenReturn(profile);
        when(encryptor.encrypt("github-token")).thenReturn("encrypted-token");
        when(githubAccountRepository.findByGithubUserId("1")).thenReturn(Optional.empty());
        when(githubAccountRepository.save(any(GithubAccount.class))).thenReturn(githubAccount);

        githubLinkService.linkGithubAccount(userId, "code");

        verify(githubAuditService).recordSuccess(
                eq(userId),
                eq(AuditEventType.GITHUB_TOKEN_LINKED),
                eq(AuditTargetType.GITHUB_ACCOUNT),
                eq(githubAccount.getGithubAccountId()),
                any()
        );
    }

    @Test
    void linkGithubAccountRejectsAlreadyLinkedCurrentUserBeforeSaving() {
        User user = User.builder()
                .username("localuser")
                .passwordHash("password")
                .provider(AuthProvider.LOCAL)
                .build();
        UUID userId = user.getUserId();
        GithubAccount githubAccount = new GithubAccount(user, "1", "octocat", "encrypted-token");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(githubAccountRepository.findByUser_UserId(userId)).thenReturn(Optional.of(githubAccount));

        assertThatThrownBy(() -> githubLinkService.linkGithubAccount(userId, "code"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.GITHUB_ACCOUNT_ALREADY_LINKED_TO_CURRENT_USER);
        verify(githubApiClient, never()).requestAccessToken(anyString());
    }

    @Test
    void saveGithubAccountRejectsAlreadyLinkedGithubAccount() {
        User currentUser = User.builder()
                .username("localuser")
                .passwordHash("password")
                .provider(AuthProvider.LOCAL)
                .build();
        User otherUser = User.builder()
                .username("otheruser")
                .passwordHash("password")
                .provider(AuthProvider.LOCAL)
                .build();
        GithubUserProfileResponse profile = new GithubUserProfileResponse(1L, "octocat");
        GithubAccount linkedAccount = new GithubAccount(otherUser, "1", "octocat", "encrypted-token");
        when(encryptor.encrypt("github-token")).thenReturn("new-encrypted-token");
        when(githubAccountRepository.findByGithubUserId("1")).thenReturn(Optional.of(linkedAccount));

        assertThatThrownBy(() -> githubLinkService.saveGithubAccount(currentUser, profile, "github-token"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.GITHUB_ACCOUNT_ALREADY_LINKED);
    }

    @Test
    void unlinkGithubAccountRejectsGithubLoginUser() {
        User githubUser = User.builder()
                .provider(AuthProvider.GITHUB)
                .providerId("1")
                .build();
        UUID userId = githubUser.getUserId();
        when(userRepository.findById(userId)).thenReturn(Optional.of(githubUser));

        assertThatThrownBy(() -> githubLinkService.unlinkGithubAccount(userId))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.GITHUB_ACCOUNT_UNLINK_NOT_ALLOWED);
        verify(githubAccountRepository, never()).deleteByUser_UserId(userId);
    }

    @Test
    void unlinkGithubAccountDeletesGithubAccountAndConnectedRepositories() {
        User localUser = User.builder()
                .username("localuser")
                .passwordHash("password")
                .provider(AuthProvider.LOCAL)
                .build();
        UUID userId = localUser.getUserId();
        GithubAccount githubAccount = new GithubAccount(localUser, "1", "octocat", "encrypted-token");
        when(userRepository.findById(userId)).thenReturn(Optional.of(localUser));
        when(githubAccountRepository.findByUser_UserId(userId)).thenReturn(Optional.of(githubAccount));

        githubLinkService.unlinkGithubAccount(userId);

        verify(connectionRepository).deleteAllByUser_UserId(userId);
        verify(githubAccountRepository).deleteByUser_UserId(userId);
    }
}
