package com.san.api.domain.auth.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.san.api.domain.auth.dto.request.LoginRequest;
import com.san.api.domain.auth.dto.request.ReissueRequest;
import com.san.api.domain.auth.dto.request.WithdrawRequest;
import com.san.api.domain.auth.dto.response.TokenResponse;
import com.san.api.domain.auth.entity.ClientType;
import com.san.api.domain.user.entity.AuthProvider;
import com.san.api.domain.user.entity.User;
import com.san.api.domain.user.entity.UserRole;
import com.san.api.domain.user.repository.UserRepository;
import com.san.api.global.audit.entity.AuditEventType;
import com.san.api.global.security.jwt.JwtProvider;
import com.san.api.global.security.jwt.JwtSessionClaims;
import com.san.api.global.security.redis.AuthRedisKeyPrefix;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AuthService의 로그인, 토큰 재발급, 로그아웃, 회원탈퇴 흐름을 검증하는 테스트.
 *
 * Dashboard/Extension clientType별 refresh token 세션 분리와
 * refresh token hash/familyId 기반 재사용 탐지 흐름도 함께 확인한다.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private SetOperations<String, String> setOperations;

    @Mock
    private TokenIssueService tokenIssueService;

    @Mock
    private AuthAuditService authAuditService;

    private AuthSessionKeyService authSessionKeyService;
    private RefreshTokenHashService refreshTokenHashService;
    private ObjectMapper objectMapper;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        authSessionKeyService = new AuthSessionKeyService();
        refreshTokenHashService = new RefreshTokenHashService("test-secret");
        objectMapper = new ObjectMapper();
        authService = new AuthService(
                userRepository,
                passwordEncoder,
                jwtProvider,
                redisTemplate,
                tokenIssueService,
                authSessionKeyService,
                refreshTokenHashService,
                objectMapper,
                authAuditService
        );
        ReflectionTestUtils.setField(authService, "maxFailCount", 5);
        ReflectionTestUtils.setField(authService, "failWindowSeconds", 300L);
        ReflectionTestUtils.setField(authService, "lockDurationSeconds", 600L);
    }

    @Test
    void loginIssuesTokenPairForRequestedClientType() {
        User user = localUser();
        TokenResponse tokenResponse = TokenResponse.of("access-token", "refresh-token", 1800, "session-id");
        when(userRepository.findByUsername("dahyeon")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", user.getPasswordHash())).thenReturn(true);
        when(tokenIssueService.issueTokenPair(user.getUserId().toString(), ClientType.EXTENSION, UserRole.USER))
                .thenReturn(tokenResponse);

        TokenResponse result = authService.login(new LoginRequest("dahyeon", "password", ClientType.EXTENSION));

        assertThat(result).isEqualTo(tokenResponse);
        verify(redisTemplate).delete(AuthRedisKeyPrefix.LOGIN_FAIL + "dahyeon");

        verify(authAuditService).recordSuccess(
                org.mockito.ArgumentMatchers.eq(user.getUserId()),
                org.mockito.ArgumentMatchers.eq(AuditEventType.LOGIN_SUCCESS),
                org.mockito.ArgumentMatchers.eq(user.getUserId()),
                org.mockito.ArgumentMatchers.argThat(metadata ->
                        "dahyeon".equals(metadata.get("username"))
                                && "EXTENSION".equals(metadata.get("clientType"))
                                && "session-id".equals(metadata.get("sessionId")))
        );
    }

    @Test
    void reissueValidatesRefreshTokenByClientSessionKey() {
        User user = localUser();
        String userId = user.getUserId().toString();
        String sessionId = "dashboard-session";
        String refreshToken = "refresh-token";
        String redisKey = AuthRedisKeyPrefix.REFRESH + userId + ":DASHBOARD:" + sessionId;
        TokenResponse tokenResponse = TokenResponse.of("new-access-token", "new-refresh-token", 1800, sessionId);

        when(jwtProvider.validateToken(refreshToken)).thenReturn(true);
        when(jwtProvider.isRefreshToken(refreshToken)).thenReturn(true);
        when(jwtProvider.getUserId(refreshToken)).thenReturn(userId);
        when(jwtProvider.getSessionClaims(refreshToken))
                .thenReturn(new JwtSessionClaims(ClientType.DASHBOARD, sessionId, "family-id", "refresh-jti", UserRole.ADMIN));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(valueOperations.getAndDelete(redisKey)).thenReturn(refreshTokenSessionJson(refreshToken, "family-id", "refresh-jti"));
        when(userRepository.findById(user.getUserId())).thenReturn(Optional.of(user));
        when(tokenIssueService.issueTokenPair(userId, ClientType.DASHBOARD, sessionId, "family-id", UserRole.USER))
                .thenReturn(tokenResponse);

        TokenResponse result = authService.reissue(new ReissueRequest(refreshToken));

        assertThat(result).isEqualTo(tokenResponse);
        verify(tokenIssueService).issueTokenPair(userId, ClientType.DASHBOARD, sessionId, "family-id", UserRole.USER);
        verify(setOperations).remove(AuthRedisKeyPrefix.REFRESH + "index:user:" + userId, redisKey);
        verify(authAuditService).recordSuccess(
                org.mockito.ArgumentMatchers.argThat(userUuid -> userUuid.toString().equals(userId)),
                org.mockito.ArgumentMatchers.eq(AuditEventType.TOKEN_REISSUE_SUCCESS),
                org.mockito.ArgumentMatchers.argThat(userUuid -> userUuid.toString().equals(userId)),
                org.mockito.ArgumentMatchers.argThat(metadata -> "family-id".equals(metadata.get("familyId")))
        );
    }

    @Test
    void reissueRevokesRefreshFamilyUsingUserSessionIndexWhenTokenHashDoesNotMatch() {
        String userId = UUID.randomUUID().toString();
        String sessionId = "dashboard-session";
        String refreshToken = "old-refresh-token";
        String redisKey = AuthRedisKeyPrefix.REFRESH + userId + ":DASHBOARD:" + sessionId;
        String familyKey = AuthRedisKeyPrefix.REFRESH + userId + ":EXTENSION:other-session";
        String otherFamilyKey = AuthRedisKeyPrefix.REFRESH + userId + ":DASHBOARD:another-session";
        String indexKey = AuthRedisKeyPrefix.REFRESH + "index:user:" + userId;

        when(jwtProvider.validateToken(refreshToken)).thenReturn(true);
        when(jwtProvider.isRefreshToken(refreshToken)).thenReturn(true);
        when(jwtProvider.getUserId(refreshToken)).thenReturn(userId);
        when(jwtProvider.getSessionClaims(refreshToken))
                .thenReturn(new JwtSessionClaims(ClientType.DASHBOARD, sessionId, "family-id", "old-jti", UserRole.USER));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(valueOperations.getAndDelete(redisKey)).thenReturn(refreshTokenSessionJson("new-refresh-token", "family-id", "new-jti"));
        when(setOperations.members(indexKey)).thenReturn(Set.of(redisKey, familyKey, otherFamilyKey));
        when(valueOperations.get(familyKey)).thenReturn(refreshTokenSessionJson("other-refresh-token", "family-id", "other-jti"));
        when(valueOperations.get(otherFamilyKey))
                .thenReturn(refreshTokenSessionJson("different-refresh-token", "other-family-id", "different-jti"));

        assertThatThrownBy(() -> authService.reissue(new ReissueRequest(refreshToken)))
                .isInstanceOf(com.san.api.global.exception.BusinessException.class);

        verify(redisTemplate).delete(familyKey);
        verify(setOperations).remove(indexKey, familyKey);
        verify(authAuditService).recordFailure(
                org.mockito.ArgumentMatchers.argThat(userUuid -> userUuid.toString().equals(userId)),
                org.mockito.ArgumentMatchers.eq(AuditEventType.TOKEN_REUSE_DETECTED),
                org.mockito.ArgumentMatchers.eq(com.san.api.global.exception.errorcode.AuthErrorCode.INVALID_REFRESH_TOKEN),
                org.mockito.ArgumentMatchers.argThat(metadata -> "family-id".equals(metadata.get("familyId")))
        );
    }

    @Test
    void loginRecordsLockTriggeredSecurityEventWhenMaxFailCountReached() {
        User user = localUser();
        when(userRepository.findByUsername("dahyeon")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", user.getPasswordHash())).thenReturn(false);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(AuthRedisKeyPrefix.LOGIN_FAIL + "dahyeon")).thenReturn(5L);

        assertThatThrownBy(() -> authService.login(new LoginRequest("dahyeon", "wrong-password", ClientType.DASHBOARD)))
                .isInstanceOf(com.san.api.global.exception.BusinessException.class);

        verify(redisTemplate).delete(AuthRedisKeyPrefix.LOGIN_FAIL + "dahyeon");
        verify(authAuditService).recordFailure(
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.eq(AuditEventType.LOGIN_LOCK_TRIGGERED),
                org.mockito.ArgumentMatchers.eq(user.getUserId()),
                org.mockito.ArgumentMatchers.eq(com.san.api.global.exception.errorcode.AuthErrorCode.ACCOUNT_LOCKED),
                org.mockito.ArgumentMatchers.argThat(metadata ->
                        Long.valueOf(5L).equals(metadata.get("failCount"))
                                && Integer.valueOf(5).equals(metadata.get("maxFailCount"))
                                && metadata.containsKey("lockedUntil"))
        );
    }

    @Test
    void logoutDeletesOnlyCurrentClientSessionRefreshToken() {
        String userId = UUID.randomUUID().toString();
        String sessionId = "extension-session";
        String accessToken = "access-token";
        String redisKey = AuthRedisKeyPrefix.REFRESH + userId + ":EXTENSION:" + sessionId;

        when(jwtProvider.validateToken(accessToken)).thenReturn(true);
        when(jwtProvider.isAccessToken(accessToken)).thenReturn(true);
        when(jwtProvider.getUserId(accessToken)).thenReturn(userId);
        when(jwtProvider.getSessionClaims(accessToken))
                .thenReturn(new JwtSessionClaims(ClientType.EXTENSION, sessionId, null, null, UserRole.USER));
        when(jwtProvider.getRemainingExpiration(accessToken)).thenReturn(1000L);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);

        authService.logout(accessToken);

        verify(redisTemplate).delete(redisKey);
        verify(setOperations).remove(AuthRedisKeyPrefix.REFRESH + "index:user:" + userId, redisKey);
        verify(valueOperations).set(AuthRedisKeyPrefix.BLACKLIST + accessToken, "1", Duration.ofMillis(1000L));
    }

    @Test
    void withdrawDeletesAllRefreshSessionsForUser() {
        User user = localUser();
        String userId = user.getUserId().toString();
        Set<String> sessionKeys = Set.of(
                AuthRedisKeyPrefix.REFRESH + userId + ":DASHBOARD:session-a",
                AuthRedisKeyPrefix.REFRESH + userId + ":EXTENSION:session-b"
        );

        when(userRepository.findById(user.getUserId())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", user.getPasswordHash())).thenReturn(true);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.members(AuthRedisKeyPrefix.REFRESH + "index:user:" + userId)).thenReturn(sessionKeys);

        authService.withdraw(userId, new WithdrawRequest("password"));

        verify(redisTemplate).delete(sessionKeys);
        verify(redisTemplate).delete(AuthRedisKeyPrefix.REFRESH + "index:user:" + userId);
    }

    private User localUser() {
        return User.builder()
                .username("dahyeon")
                .passwordHash("encoded-password")
                .provider(AuthProvider.LOCAL)
                .build();
    }

    private String refreshTokenSessionJson(String refreshToken, String familyId, String jti) {
        try {
            return objectMapper.writeValueAsString(new RefreshTokenSession(
                    refreshTokenHashService.hash(refreshToken),
                    familyId,
                    jti
            ));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }
}
