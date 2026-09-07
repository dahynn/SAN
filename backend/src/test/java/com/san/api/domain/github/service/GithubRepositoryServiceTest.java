package com.san.api.domain.github.service;

import com.san.api.domain.github.dto.request.GithubRepositoryConnectRequest;
import com.san.api.domain.github.dto.response.GithubRepositoryResponse;
import com.san.api.domain.github.entity.GithubAccount;
import com.san.api.domain.github.entity.GithubRepositoryConnection;
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
import com.san.api.global.external.github.dto.response.ExternalGithubRepositoryResponse;
import com.san.api.global.security.crypto.AesGcmStringEncryptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** GitHub 레포지토리 조회와 연결 서비스 동작을 검증하는 테스트. */
@ExtendWith(MockitoExtension.class)
class GithubRepositoryServiceTest {

    @Mock
    private GithubApiClient githubApiClient;

    @Mock
    private GithubAccountRepository githubAccountRepository;

    @Mock
    private GithubRepositoryConnectionRepository connectionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AesGcmStringEncryptor encryptor;

    @Mock
    private GithubAuditService githubAuditService;

    private GithubRepositoryService githubRepositoryService;
    private User user;
    private UUID userId;
    private GithubAccount githubAccount;
    private ExternalGithubRepositoryResponse repository;

    @BeforeEach
    void setUp() {
        githubRepositoryService = new GithubRepositoryService(
                githubApiClient,
                githubAccountRepository,
                connectionRepository,
                userRepository,
                encryptor,
                githubAuditService
        );
        user = User.builder()
                .provider(AuthProvider.GITHUB)
                .providerId("123")
                .build();
        userId = user.getUserId();
        githubAccount = new GithubAccount(user, "123", "octocat", "encrypted-token");
        repository = new ExternalGithubRepositoryResponse(
                100L,
                "algorithm",
                "octocat/algorithm",
                false,
                "main",
                "https://github.com/octocat/algorithm"
        );
    }

    @Test
    void findRepositoriesUsesStoredGithubToken() {
        mockGithubRepositoryLookup();

        List<GithubRepositoryResponse> responses = githubRepositoryService.findRepositories(userId);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).githubRepositoryId()).isEqualTo(100L);
        assertThat(responses.get(0).fullName()).isEqualTo("octocat/algorithm");
        verify(encryptor).decrypt("encrypted-token");
        verify(githubApiClient).findRepositories("plain-token");
        verify(githubAuditService).recordSuccess(
                eq(userId),
                eq(AuditEventType.GITHUB_API_SUCCEEDED),
                eq(AuditTargetType.GITHUB_REPOSITORY),
                eq(userId),
                any()
        );
    }

    @Test
    void connectRepositoryStoresOnlyRepositoryAccessibleFromGithub() {
        mockGithubRepositoryLookup();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(connectionRepository.findByUser_UserId(userId)).thenReturn(Optional.empty());
        when(connectionRepository.save(any(GithubRepositoryConnection.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        GithubRepositoryResponse response = githubRepositoryService.connectRepository(
                userId,
                new GithubRepositoryConnectRequest(100L)
        );

        assertThat(response.githubRepositoryId()).isEqualTo(100L);
        assertThat(response.name()).isEqualTo("algorithm");
        assertThat(response.defaultBranch()).isEqualTo("main");
        verify(connectionRepository, never()).deleteAllByUser_UserId(userId);
        verify(connectionRepository).save(any(GithubRepositoryConnection.class));
        verify(githubAuditService).recordSuccess(
                eq(userId),
                eq(AuditEventType.GITHUB_API_SUCCEEDED),
                eq(AuditTargetType.GITHUB_REPOSITORY),
                any(UUID.class),
                any()
        );
    }

    @Test
    void connectRepositoryUpdatesExistingConnection() {
        mockGithubRepositoryLookup();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        GithubRepositoryConnection existingConnection = new GithubRepositoryConnection(user, new ExternalGithubRepositoryResponse(
                200L,
                "old",
                "octocat/old",
                false,
                "develop",
                "https://github.com/octocat/old"
        ));
        when(connectionRepository.findByUser_UserId(userId)).thenReturn(Optional.of(existingConnection));

        GithubRepositoryResponse response = githubRepositoryService.connectRepository(
                userId,
                new GithubRepositoryConnectRequest(100L)
        );

        assertThat(response.githubRepositoryId()).isEqualTo(100L);
        assertThat(response.fullName()).isEqualTo("octocat/algorithm");
        verify(connectionRepository, never()).deleteAllByUser_UserId(userId);
        verify(connectionRepository, never()).save(any(GithubRepositoryConnection.class));
    }

    @Test
    void connectRepositoryRejectsRepositoryNotReturnedByGithub() {
        mockGithubRepositoryLookup();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> githubRepositoryService.connectRepository(
                userId,
                new GithubRepositoryConnectRequest(999L)
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.GITHUB_REPOSITORY_NOT_FOUND);
        verify(githubAuditService).recordFailure(
                eq(userId),
                eq(AuditEventType.GITHUB_API_FAILED),
                eq(AuditTargetType.GITHUB_REPOSITORY),
                eq(userId),
                eq(AuthErrorCode.GITHUB_REPOSITORY_NOT_FOUND),
                any()
        );
    }

    private void mockGithubRepositoryLookup() {
        when(githubAccountRepository.findByUser_UserId(userId)).thenReturn(Optional.of(githubAccount));
        when(encryptor.decrypt("encrypted-token")).thenReturn("plain-token");
        when(githubApiClient.findRepositories("plain-token")).thenReturn(List.of(repository));
    }
}
