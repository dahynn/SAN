package com.san.api.domain.github.service;

import com.san.api.domain.github.dto.response.GithubLinkStatusResponse;
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
import com.san.api.global.exception.errorcode.CommonErrorCode;
import com.san.api.global.external.github.client.GithubApiClient;
import com.san.api.global.external.github.dto.response.GithubAccessTokenResponse;
import com.san.api.global.external.github.dto.response.GithubUserProfileResponse;
import com.san.api.global.security.crypto.AesGcmStringEncryptor;
import com.san.api.global.security.redis.AuthRedisKeyPrefix;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** 로그인된 서비스 사용자와 GitHub 계정의 연동 흐름을 담당하는 서비스. */
@Service
@RequiredArgsConstructor
public class GithubLinkService {

    private static final Duration STATE_TTL = Duration.ofMinutes(5);

    private final GithubApiClient githubApiClient;
    private final UserRepository userRepository;
    private final GithubAccountRepository githubAccountRepository;
    private final GithubRepositoryConnectionRepository connectionRepository;
    private final StringRedisTemplate redisTemplate;
    private final AesGcmStringEncryptor encryptor;
    private final GithubAuditService githubAuditService;
    private final SecureRandom secureRandom = new SecureRandom();

    public String createLinkAuthorizationRedirectUrl(UUID userId) {
        if (!userRepository.existsById(userId)) {
            throw new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND);
        }

        String state = generateUrlSafeToken();
        redisTemplate.opsForValue().set(
                AuthRedisKeyPrefix.GITHUB_LINK_STATE + state,
                userId.toString(),
                STATE_TTL
        );
        return githubApiClient.createAuthorizationUrl(state);
    }

    public Optional<UUID> consumeLinkUserId(String state) {
        String key = AuthRedisKeyPrefix.GITHUB_LINK_STATE + state;
        String userId = redisTemplate.opsForValue().get(key);
        if (userId == null) {
            return Optional.empty();
        }

        redisTemplate.delete(key);
        return Optional.of(UUID.fromString(userId));
    }

    @Transactional(readOnly = true)
    public GithubLinkStatusResponse getLinkStatus(UUID userId) {
        Optional<GithubAccount> githubAccount = githubAccountRepository.findByUser_UserId(userId);
        if (githubAccount.isEmpty()) {
            return GithubLinkStatusResponse.notLinked();
        }

        GithubRepositoryConnection repositoryConnection = connectionRepository.findByUser_UserId(userId)
                .orElse(null);
        return GithubLinkStatusResponse.from(githubAccount.get(), repositoryConnection);
    }

    @Transactional
    public void linkGithubAccount(UUID userId, String code) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND));
            if (githubAccountRepository.findByUser_UserId(userId).isPresent()) {
                throw new BusinessException(AuthErrorCode.GITHUB_ACCOUNT_ALREADY_LINKED_TO_CURRENT_USER);
            }

            GithubAccessTokenResponse tokenResponse = githubApiClient.requestAccessToken(code);
            GithubUserProfileResponse profile = githubApiClient.findUserProfile(tokenResponse.accessToken());

            GithubAccount account = saveGithubAccount(user, profile, tokenResponse.accessToken());
            githubAuditService.recordSuccess(
                    userId,
                    AuditEventType.GITHUB_TOKEN_LINKED,
                    AuditTargetType.GITHUB_ACCOUNT,
                    account.getGithubAccountId(),
                    githubAccountMetadata(profile.login(), profile.id().toString())
            );
        } catch (BusinessException e) {
            githubAuditService.recordFailure(
                    userId,
                    AuditEventType.GITHUB_API_FAILED,
                    AuditTargetType.GITHUB_ACCOUNT,
                    userId,
                    e.getErrorCode(),
                    Map.of("operation", "LINK_ACCOUNT")
            );
            throw e;
        } catch (RuntimeException e) {
            githubAuditService.recordFailure(
                    userId,
                    AuditEventType.GITHUB_API_FAILED,
                    AuditTargetType.GITHUB_ACCOUNT,
                    userId,
                    null,
                    Map.of("operation", "LINK_ACCOUNT", "exceptionType", e.getClass().getSimpleName())
            );
            throw e;
        }
    }

    @Transactional
    public GithubAccount saveGithubAccount(User user, GithubUserProfileResponse profile, String accessToken) {
        String githubUserId = profile.id().toString();
        String encryptedToken = encryptor.encrypt(accessToken);

        return githubAccountRepository.findByGithubUserId(githubUserId)
                .map(account -> updateOwnAccount(account, user, profile.login(), encryptedToken))
                .orElseGet(() -> githubAccountRepository.save(
                        new GithubAccount(user, githubUserId, profile.login(), encryptedToken)
                ));
    }

    @Transactional
    public void unlinkGithubAccount(UUID userId) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND));
            if (user.getProvider() == AuthProvider.GITHUB) {
                throw new BusinessException(AuthErrorCode.GITHUB_ACCOUNT_UNLINK_NOT_ALLOWED);
            }

            GithubAccount githubAccount = githubAccountRepository.findByUser_UserId(userId)
                    .orElseThrow(() -> new BusinessException(AuthErrorCode.GITHUB_ACCOUNT_NOT_LINKED));

            connectionRepository.deleteAllByUser_UserId(userId);
            githubAccountRepository.deleteByUser_UserId(userId);
            githubAuditService.recordSuccess(
                    userId,
                    AuditEventType.GITHUB_API_SUCCEEDED,
                    AuditTargetType.GITHUB_ACCOUNT,
                    githubAccount.getGithubAccountId(),
                    githubAccountMetadata(githubAccount.getGithubUsername(), githubAccount.getGithubUserId(),
                            "UNLINK_ACCOUNT")
            );
        } catch (BusinessException e) {
            githubAuditService.recordFailure(
                    userId,
                    AuditEventType.GITHUB_API_FAILED,
                    AuditTargetType.GITHUB_ACCOUNT,
                    userId,
                    e.getErrorCode(),
                    Map.of("operation", "UNLINK_ACCOUNT")
            );
            throw e;
        }
    }

    private GithubAccount updateOwnAccount(GithubAccount account, User user, String githubUsername, String encryptedToken) {
        if (!account.getUser().getUserId().equals(user.getUserId())) {
            throw new BusinessException(AuthErrorCode.GITHUB_ACCOUNT_ALREADY_LINKED);
        }
        account.updateToken(githubUsername, encryptedToken);
        return account;
    }

    private String generateUrlSafeToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private Map<String, Object> githubAccountMetadata(String githubUsername, String githubUserId) {
        return githubAccountMetadata(githubUsername, githubUserId, "LINK_ACCOUNT");
    }

    private Map<String, Object> githubAccountMetadata(String githubUsername, String githubUserId, String operation) {
        return Map.of(
                "operation", operation,
                "githubUsername", githubUsername,
                "githubUserId", githubUserId
        );
    }
}
