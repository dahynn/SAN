package com.san.api.domain.auth.service;

import com.san.api.domain.auth.dto.response.AuthSessionListResponse;
import com.san.api.domain.auth.entity.ClientType;
import com.san.api.domain.user.entity.UserRole;
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

import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AuthSessionService의 인증 세션 목록 조회와 개별 세션 폐기 동작을 검증합니다.
 *
 * Redis에 저장된 refresh token 세션 인덱스를 기준으로 현재 세션 표시, 만료 세션 제외,
 * 현재 세션 폐기 시 access token blacklist 처리를 함께 확인합니다.
 */
@ExtendWith(MockitoExtension.class)
class AuthSessionServiceTest {

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private SetOperations<String, String> setOperations;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private AuthAuditService authAuditService;

    private AuthSessionKeyService authSessionKeyService;
    private AuthSessionService authSessionService;

    @BeforeEach
    void setUp() {
        authSessionKeyService = new AuthSessionKeyService();
        authSessionService = new AuthSessionService(jwtProvider, redisTemplate, authSessionKeyService, authAuditService);
    }

    @Test
    void getSessionsReturnsUserRefreshSessionsWithCurrentFlag() {
        String userId = UUID.randomUUID().toString();
        String accessToken = "access-token";
        String currentKey = AuthRedisKeyPrefix.REFRESH + userId + ":DASHBOARD:current-session";
        String otherKey = AuthRedisKeyPrefix.REFRESH + userId + ":EXTENSION:other-session";
        String staleKey = AuthRedisKeyPrefix.REFRESH + userId + ":DASHBOARD:stale-session";
        String indexKey = AuthRedisKeyPrefix.REFRESH + "index:user:" + userId;

        when(jwtProvider.validateToken(accessToken)).thenReturn(true);
        when(jwtProvider.isAccessToken(accessToken)).thenReturn(true);
        when(jwtProvider.getUserId(accessToken)).thenReturn(userId);
        when(jwtProvider.getSessionClaims(accessToken))
                .thenReturn(new JwtSessionClaims(ClientType.DASHBOARD, "current-session", null, null, UserRole.USER));
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.members(indexKey)).thenReturn(Set.of(currentKey, otherKey, staleKey));
        when(redisTemplate.hasKey(currentKey)).thenReturn(true);
        when(redisTemplate.hasKey(otherKey)).thenReturn(true);
        when(redisTemplate.hasKey(staleKey)).thenReturn(false);
        when(redisTemplate.getExpire(currentKey, TimeUnit.SECONDS)).thenReturn(3600L);
        when(redisTemplate.getExpire(otherKey, TimeUnit.SECONDS)).thenReturn(1800L);

        AuthSessionListResponse response = authSessionService.getSessions(accessToken);

        assertThat(response.sessions()).hasSize(2);
        assertThat(response.sessions().get(0).sessionId()).isEqualTo("current-session");
        assertThat(response.sessions().get(0).clientType()).isEqualTo(ClientType.DASHBOARD);
        assertThat(response.sessions().get(0).current()).isTrue();
        assertThat(response.sessions().get(0).expiresInSeconds()).isEqualTo(3600L);
        assertThat(response.sessions().get(1).sessionId()).isEqualTo("other-session");
        assertThat(response.sessions().get(1).clientType()).isEqualTo(ClientType.EXTENSION);
        assertThat(response.sessions().get(1).current()).isFalse();
        assertThat(response.sessions().get(1).expiresInSeconds()).isEqualTo(1800L);
    }

    @Test
    void revokeSessionDeletesRefreshSessionAndIndexEntry() {
        String userId = UUID.randomUUID().toString();
        String accessToken = "access-token";
        String refreshKey = AuthRedisKeyPrefix.REFRESH + userId + ":EXTENSION:session-id";
        String indexKey = AuthRedisKeyPrefix.REFRESH + "index:user:" + userId;

        when(jwtProvider.validateToken(accessToken)).thenReturn(true);
        when(jwtProvider.isAccessToken(accessToken)).thenReturn(true);
        when(jwtProvider.getUserId(accessToken)).thenReturn(userId);
        when(jwtProvider.getSessionClaims(accessToken))
                .thenReturn(new JwtSessionClaims(ClientType.DASHBOARD, "current-session", null, null, UserRole.USER));
        when(redisTemplate.opsForSet()).thenReturn(setOperations);

        authSessionService.revokeSession(accessToken, ClientType.EXTENSION, "session-id");

        verify(redisTemplate).delete(refreshKey);
        verify(setOperations).remove(indexKey, refreshKey);
        verify(authAuditService).recordSuccess(
                org.mockito.ArgumentMatchers.argThat(userUuid -> userUuid.toString().equals(userId)),
                org.mockito.ArgumentMatchers.eq(AuditEventType.SESSION_REVOKED),
                org.mockito.ArgumentMatchers.argThat(userUuid -> userUuid.toString().equals(userId)),
                org.mockito.ArgumentMatchers.argThat(metadata ->
                        "EXTENSION".equals(metadata.get("clientType"))
                                && "session-id".equals(metadata.get("sessionId"))
                                && Boolean.FALSE.equals(metadata.get("currentSession")))
        );
    }

    @Test
    void revokeCurrentSessionBlacklistsCurrentAccessToken() {
        String userId = UUID.randomUUID().toString();
        String accessToken = "access-token";
        String refreshKey = AuthRedisKeyPrefix.REFRESH + userId + ":DASHBOARD:current-session";
        String indexKey = AuthRedisKeyPrefix.REFRESH + "index:user:" + userId;

        when(jwtProvider.validateToken(accessToken)).thenReturn(true);
        when(jwtProvider.isAccessToken(accessToken)).thenReturn(true);
        when(jwtProvider.getUserId(accessToken)).thenReturn(userId);
        when(jwtProvider.getSessionClaims(accessToken))
                .thenReturn(new JwtSessionClaims(ClientType.DASHBOARD, "current-session", null, null, UserRole.USER));
        when(jwtProvider.getRemainingExpiration(accessToken)).thenReturn(1000L);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        authSessionService.revokeSession(accessToken, ClientType.DASHBOARD, "current-session");

        verify(redisTemplate).delete(refreshKey);
        verify(setOperations).remove(indexKey, refreshKey);
        verify(valueOperations).set(AuthRedisKeyPrefix.BLACKLIST + accessToken, "1", Duration.ofMillis(1000L));
        verify(authAuditService).recordSuccess(
                org.mockito.ArgumentMatchers.argThat(userUuid -> userUuid.toString().equals(userId)),
                org.mockito.ArgumentMatchers.eq(AuditEventType.SESSION_REVOKED),
                org.mockito.ArgumentMatchers.argThat(userUuid -> userUuid.toString().equals(userId)),
                org.mockito.ArgumentMatchers.argThat(metadata ->
                        "DASHBOARD".equals(metadata.get("clientType"))
                                && "current-session".equals(metadata.get("sessionId"))
                                && Boolean.TRUE.equals(metadata.get("currentSession")))
        );
    }
}
