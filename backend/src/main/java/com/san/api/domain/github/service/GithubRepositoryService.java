package com.san.api.domain.github.service;

import com.san.api.domain.github.dto.request.GithubRepositoryConnectRequest;
import com.san.api.domain.github.dto.response.GithubRepositoryResponse;
import com.san.api.domain.github.entity.GithubAccount;
import com.san.api.domain.github.entity.GithubRepositoryConnection;
import com.san.api.domain.github.repository.GithubAccountRepository;
import com.san.api.domain.github.repository.GithubRepositoryConnectionRepository;
import com.san.api.domain.user.entity.User;
import com.san.api.domain.user.repository.UserRepository;
import com.san.api.global.audit.entity.AuditEventType;
import com.san.api.global.audit.entity.AuditTargetType;
import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.AuthErrorCode;
import com.san.api.global.exception.errorcode.CommonErrorCode;
import com.san.api.global.external.github.client.GithubApiClient;
import com.san.api.global.external.github.dto.response.ExternalGithubRepositoryResponse;
import com.san.api.global.security.crypto.AesGcmStringEncryptor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** GitHub 레포지토리 목록 조회와 서비스 계정 연결을 담당하는 서비스. */
@Service
@RequiredArgsConstructor
public class GithubRepositoryService {

    private final GithubApiClient githubApiClient;
    private final GithubAccountRepository githubAccountRepository;
    private final GithubRepositoryConnectionRepository connectionRepository;
    private final UserRepository userRepository;
    private final AesGcmStringEncryptor encryptor;
    private final GithubAuditService githubAuditService;

    /** 로그인 사용자의 GitHub access token으로 접근 가능한 저장소 목록을 조회합니다. */
    @Transactional(readOnly = true)
    public List<GithubRepositoryResponse> findRepositories(UUID userId) {
        try {
            List<GithubRepositoryResponse> repositories = findGithubRepositories(userId).stream()
                    .map(GithubRepositoryResponse::from)
                    .toList();
            githubAuditService.recordSuccess(
                    userId,
                    AuditEventType.GITHUB_API_SUCCEEDED,
                    AuditTargetType.GITHUB_REPOSITORY,
                    userId,
                    Map.of("operation", "LIST_REPOSITORIES", "repositoryCount", repositories.size())
            );
            return repositories;
        } catch (BusinessException e) {
            githubAuditService.recordFailure(
                    userId,
                    AuditEventType.GITHUB_API_FAILED,
                    AuditTargetType.GITHUB_REPOSITORY,
                    userId,
                    e.getErrorCode(),
                    Map.of("operation", "LIST_REPOSITORIES")
            );
            throw e;
        }
    }

    /**
     * 사용자가 선택한 GitHub 저장소를 서비스 계정의 단일 TIL 커밋 저장소로 연결합니다.
     *
     * 클라이언트를 통해 받은 저장소 ID를 신뢰하지 않고 GitHub API 목록에서 다시 조회해,
     * 사용자가 실제 접근 가능한 저장소만 연결합니다.
     */
    @Transactional
    public GithubRepositoryResponse connectRepository(UUID userId, GithubRepositoryConnectRequest request) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND));

            ExternalGithubRepositoryResponse repository = findGithubRepositories(userId).stream()
                    .filter(item -> item.id().equals(request.githubRepositoryId()))
                    .findFirst()
                    .orElseThrow(() -> new BusinessException(AuthErrorCode.GITHUB_REPOSITORY_NOT_FOUND));

            GithubRepositoryConnection connection = connectionRepository.findByUser_UserId(userId)
                    .map(existing -> {
                        existing.update(repository);
                        return existing;
                    })
                    .orElseGet(() -> connectionRepository.save(new GithubRepositoryConnection(user, repository)));

            githubAuditService.recordSuccess(
                    userId,
                    AuditEventType.GITHUB_API_SUCCEEDED,
                    AuditTargetType.GITHUB_REPOSITORY,
                    connection.getGithubRepositoryConnectionId(),
                    repositoryMetadata("CONNECT_REPOSITORY", connection)
            );
            return GithubRepositoryResponse.from(connection);
        } catch (BusinessException e) {
            githubAuditService.recordFailure(
                    userId,
                    AuditEventType.GITHUB_API_FAILED,
                    AuditTargetType.GITHUB_REPOSITORY,
                    userId,
                    e.getErrorCode(),
                    Map.of(
                            "operation", "CONNECT_REPOSITORY",
                            "requestedGithubRepositoryId", request.githubRepositoryId()
                    )
            );
            throw e;
        }
    }

    /** 서비스에 연결된 GitHub 저장소를 해제합니다. */
    @Transactional
    public void disconnectRepository(UUID userId, Long repositoryId) {
        try {
            GithubRepositoryConnection connection = connectionRepository
                    .findByUser_UserIdAndGithubRepositoryId(userId, repositoryId)
                    .orElseThrow(() -> new BusinessException(AuthErrorCode.GITHUB_REPOSITORY_NOT_FOUND));

            connectionRepository.delete(connection);
            githubAuditService.recordSuccess(
                    userId,
                    AuditEventType.GITHUB_API_SUCCEEDED,
                    AuditTargetType.GITHUB_REPOSITORY,
                    connection.getGithubRepositoryConnectionId(),
                    repositoryMetadata("DISCONNECT_REPOSITORY", connection)
            );
        } catch (BusinessException e) {
            githubAuditService.recordFailure(
                    userId,
                    AuditEventType.GITHUB_API_FAILED,
                    AuditTargetType.GITHUB_REPOSITORY,
                    userId,
                    e.getErrorCode(),
                    Map.of(
                            "operation", "DISCONNECT_REPOSITORY",
                            "githubRepositoryId", repositoryId
                    )
            );
            throw e;
        }
    }

    private List<ExternalGithubRepositoryResponse> findGithubRepositories(UUID userId) {
        GithubAccount githubAccount = githubAccountRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new BusinessException(AuthErrorCode.GITHUB_ACCOUNT_NOT_LINKED));
        String accessToken = encryptor.decrypt(githubAccount.getAccessTokenEncrypted());
        return githubApiClient.findRepositories(accessToken);
    }

    private Map<String, Object> repositoryMetadata(String operation, GithubRepositoryConnection connection) {
        return Map.of(
                "operation", operation,
                "githubRepositoryId", connection.getGithubRepositoryId(),
                "repositoryName", connection.getName(),
                "repositoryFullName", connection.getFullName(),
                "defaultBranch", connection.getDefaultBranch(),
                "privateRepository", connection.isPrivateRepository()
        );
    }
}
