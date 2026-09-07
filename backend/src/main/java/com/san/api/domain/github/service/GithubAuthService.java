package com.san.api.domain.github.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.san.api.domain.auth.dto.response.TokenResponse;
import com.san.api.domain.auth.entity.ClientType;
import com.san.api.domain.auth.service.TokenIssueService;
import com.san.api.domain.github.dto.request.GithubLoginRequest;
import com.san.api.domain.github.dto.request.GithubTokenExchangeRequest;
import com.san.api.domain.github.entity.GithubAccount;
import com.san.api.domain.github.repository.GithubAccountRepository;
import com.san.api.domain.user.entity.AuthProvider;
import com.san.api.domain.user.entity.User;
import com.san.api.domain.user.repository.UserRepository;
import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.AuthErrorCode;
import com.san.api.global.exception.errorcode.CommonErrorCode;
import com.san.api.global.external.github.client.GithubApiClient;
import com.san.api.global.external.github.dto.response.GithubAccessTokenResponse;
import com.san.api.global.external.github.dto.response.GithubUserProfileResponse;
import com.san.api.global.security.redis.AuthRedisKeyPrefix;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

/** GitHub OAuth 로그인, callback 처리, 서비스 JWT 발급을 담당하는 서비스. */
@Service
@RequiredArgsConstructor
public class GithubAuthService {

    private static final Duration STATE_TTL = Duration.ofMinutes(5);
    private static final Duration LOGIN_TICKET_TTL = Duration.ofMinutes(2);

    private final GithubApiClient githubApiClient;
    private final UserRepository userRepository;
    private final GithubAccountRepository githubAccountRepository;
    private final TokenIssueService tokenIssueService;
    private final GithubLinkService githubLinkService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${oauth.github.success-redirect-uri}")
    private String successRedirectUri;

    @Value("${oauth.github.failure-redirect-uri}")
    private String failureRedirectUri;

    /**
     * 기본 GitHub OAuth 실패 redirect URL을 생성합니다.
     *
     * @return 기본 GitHub OAuth 실패 redirect URL
     */
    public String createFailureRedirectUrl() {
        return UriComponentsBuilder.fromUriString(failureRedirectUri)
                .queryParam("error", AuthErrorCode.GITHUB_OAUTH_FAILED.getCode())
                .build()
                .toUriString();
    }

    /**
     * GitHub OAuth authorize URL을 생성합니다.
     *
     * state는 Redis에 짧게 저장해 callback 요청이 백엔드가 시작한 OAuth 흐름인지 검증합니다.
     */
    public String createAuthorizationRedirectUrl(ClientType clientType) {
        String state = generateUrlSafeToken();
        redisTemplate.opsForValue().set(AuthRedisKeyPrefix.GITHUB_OAUTH_STATE + state, clientType.name(), STATE_TTL);
        return githubApiClient.createAuthorizationUrl(state);
    }

    /**
     * GitHub OAuth callback을 처리하고 프론트엔드로 돌려보낼 redirect URL을 생성합니다.
     *
     * JWT 토큰은 URL에 노출하지 않고 Redis에 저장된 일회용 ticket으로 간접 전달합니다.
     */
    @Transactional
    public String handleCallback(String code, String state) {
        try {
            Optional<UUID> linkUserId = githubLinkService.consumeLinkUserId(state);
            if (linkUserId.isPresent()) {
                githubLinkService.linkGithubAccount(linkUserId.get(), code);
                return UriComponentsBuilder.fromUriString(successRedirectUri)
                        .queryParam("githubLinked", true)
                        .build()
                        .toUriString();
            }

            ClientType clientType = validateState(state);
            TokenResponse tokens = loginWithCode(code, clientType);
            String ticket = saveLoginTicket(tokens);
            return UriComponentsBuilder.fromUriString(successRedirectUri)
                    .queryParam("ticket", ticket)
                    .build()
                    .toUriString();
        } catch (BusinessException e) {
            return createFailureRedirectUrl(e.getErrorCode().getCode());
        }
    }

    /**
     * 지정된 에러 코드를 포함한 GitHub OAuth 실패 redirect URL을 생성합니다.
     *
     * @param errorCode 프론트엔드에 전달할 에러 코드
     * @return GitHub OAuth 실패 redirect URL
     */
    private String createFailureRedirectUrl(String errorCode) {
        return UriComponentsBuilder.fromUriString(failureRedirectUri)
                .queryParam("error", errorCode)
                .build()
                .toUriString();
    }

    /** 일회용 GitHub 로그인 ticket을 서비스 JWT 토큰 쌍으로 교환합니다. */
    public TokenResponse exchangeToken(GithubTokenExchangeRequest request) {
        String key = AuthRedisKeyPrefix.GITHUB_LOGIN_TICKET + request.ticket();
        String tokenJson = redisTemplate.opsForValue().get(key);
        if (tokenJson == null) {
            throw new BusinessException(AuthErrorCode.GITHUB_OAUTH_FAILED);
        }

        redisTemplate.delete(key);
        try {
            return objectMapper.readValue(tokenJson, TokenResponse.class);
        } catch (JsonProcessingException e) {
            throw new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * GitHub authorization code를 검증하고 서비스 JWT 토큰 쌍을 발급합니다.
     *
     * GitHub 고유 사용자 ID를 {@code providerId}로 사용해 기존 계정을 조회하고,
     * 최초 로그인 사용자는 비밀번호 없는 GitHub 계정으로 자동 가입시킵니다.
     */
    @Transactional
    public TokenResponse login(GithubLoginRequest request) {
        return loginWithCode(request.code(), request.clientType());
    }

    private TokenResponse loginWithCode(String code, ClientType clientType) {
        GithubAccessTokenResponse tokenResponse = githubApiClient.requestAccessToken(code);
        GithubUserProfileResponse profile = githubApiClient.findUserProfile(tokenResponse.accessToken());
        String githubUserId = profile.id().toString();

        User user = githubAccountRepository.findByGithubUserId(githubUserId)
                .map(GithubAccount::getUser)
                .orElseGet(() -> userRepository.findByProviderAndProviderId(AuthProvider.GITHUB, githubUserId)
                        .orElseGet(() -> createGithubUser(githubUserId)));

        githubLinkService.saveGithubAccount(user, profile, tokenResponse.accessToken());
        validateLoginAvailable(user);
        return tokenIssueService.issueTokenPair(user.getUserId().toString(), clientType, user.getRole());
    }

    private User createGithubUser(String githubUserId) {
        User user = User.builder()
                .provider(AuthProvider.GITHUB)
                .providerId(githubUserId)
                .build();

        return userRepository.save(user);
    }

    private ClientType validateState(String state) {
        String key = AuthRedisKeyPrefix.GITHUB_OAUTH_STATE + state;
        String stored = redisTemplate.opsForValue().get(key);
        if (stored == null) {
            throw new BusinessException(AuthErrorCode.GITHUB_OAUTH_FAILED);
        }
        redisTemplate.delete(key);
        return ClientType.from(stored);
    }

    private String saveLoginTicket(TokenResponse tokens) {
        String ticket = generateUrlSafeToken();
        try {
            redisTemplate.opsForValue().set(
                    AuthRedisKeyPrefix.GITHUB_LOGIN_TICKET + ticket,
                    objectMapper.writeValueAsString(tokens),
                    LOGIN_TICKET_TTL
            );
            return ticket;
        } catch (JsonProcessingException e) {
            throw new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private String generateUrlSafeToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private void validateLoginAvailable(User user) {
        if (user.isWithdrawn()) {
            throw new BusinessException(AuthErrorCode.ACCOUNT_WITHDRAWN);
        }

        user.unlockIfExpired();
        if (user.isLocked()) {
            throw new BusinessException(AuthErrorCode.ACCOUNT_LOCKED);
        }
    }
}
