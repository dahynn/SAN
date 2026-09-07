package com.san.api.domain.auth.service;

import com.san.api.domain.auth.dto.response.AuthSessionListResponse;
import com.san.api.domain.auth.entity.ClientType;
import com.san.api.global.audit.entity.AuditEventType;
import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.AuthErrorCode;
import com.san.api.global.security.jwt.JwtProvider;
import com.san.api.global.security.jwt.JwtSessionClaims;
import com.san.api.global.security.redis.AuthRedisKeyPrefix;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AuthSessionService {

    private static final int SESSION_KEY_PART_COUNT = 3;

    private final JwtProvider jwtProvider;
    private final StringRedisTemplate redisTemplate;
    private final AuthSessionKeyService authSessionKeyService;
    private final AuthAuditService authAuditService;

    /**
     * Access Token의 사용자 식별자를 기준으로 Redis에 저장된 refresh token 세션 목록을 조회합니다.
     * 만료되어 실제 refresh key가 사라진 인덱스 항목은 응답에서 제외합니다.
     *
     * @param accessToken 현재 요청의 Bearer access token
     * @return 현재 세션 여부와 남은 만료 시간을 포함한 인증 세션 목록
     */
    public AuthSessionListResponse getSessions(String accessToken) {
        if (!jwtProvider.validateToken(accessToken) || !jwtProvider.isAccessToken(accessToken)) {
            throw new BusinessException(AuthErrorCode.INVALID_ACCESS_TOKEN);
        }

        String userId = jwtProvider.getUserId(accessToken);
        JwtSessionClaims currentSession = jwtProvider.getSessionClaims(accessToken);
        String currentSessionId = requireSessionId(currentSession.sessionId());
        ClientType currentClientType = currentSession.clientType();
        String indexKey = authSessionKeyService.userRefreshIndexKey(userId);
        Set<String> keys = redisTemplate.opsForSet().members(indexKey);

        if (keys == null || keys.isEmpty()) {
            return AuthSessionListResponse.from(List.of());
        }

        List<AuthSessionListResponse.Session> sessions = keys.stream()
                .map(this::parseSessionKey)
                .filter(session -> session != null)
                .filter(session -> Boolean.TRUE.equals(redisTemplate.hasKey(session.refreshKey())))
                .map(session -> AuthSessionListResponse.Session.of(
                        session.sessionId(),
                        session.clientType(),
                        session.isCurrent(currentClientType, currentSessionId),
                        refreshTtlSeconds(session.refreshKey())
                ))
                .sorted(Comparator
                        .comparing(AuthSessionListResponse.Session::current).reversed()
                        .thenComparing(AuthSessionListResponse.Session::clientType)
                        .thenComparing(AuthSessionListResponse.Session::sessionId))
                .toList();

        return AuthSessionListResponse.from(sessions);
    }

    /**
     * 요청 사용자의 특정 클라이언트 인증 세션을 폐기합니다.
     * 대상 세션의 refresh token 저장 값과 사용자별 세션 인덱스를 함께 제거합니다.
     *
     * @param accessToken 현재 요청의 Bearer access token
     * @param clientType 폐기할 세션의 클라이언트 유형
     * @param sessionId 폐기할 세션 식별자
     */
    public void revokeSession(String accessToken, ClientType clientType, String sessionId) {
        if (!jwtProvider.validateToken(accessToken) || !jwtProvider.isAccessToken(accessToken)) {
            authAuditService.recordFailure(
                    null,
                    AuditEventType.SESSION_REVOKED,
                    AuthErrorCode.INVALID_ACCESS_TOKEN,
                    revokeMetadata(clientType, sessionId, false)
            );
            throw new BusinessException(AuthErrorCode.INVALID_ACCESS_TOKEN);
        }

        String userId = jwtProvider.getUserId(accessToken);
        UUID userUuid = UUID.fromString(userId);
        String refreshKey = authSessionKeyService.refreshKey(userId, clientType, sessionId);
        redisTemplate.delete(refreshKey);
        redisTemplate.opsForSet().remove(authSessionKeyService.userRefreshIndexKey(userId), refreshKey);

        JwtSessionClaims currentSession = jwtProvider.getSessionClaims(accessToken);
        String currentSessionId = requireSessionId(currentSession.sessionId());
        boolean currentSessionRevoked = clientType == currentSession.clientType() && sessionId.equals(currentSessionId);
        if (currentSessionRevoked) {
            blacklistCurrentAccessToken(accessToken);
        }
        authAuditService.recordSuccess(
                userUuid,
                AuditEventType.SESSION_REVOKED,
                userUuid,
                revokeMetadata(clientType, sessionId, currentSessionRevoked)
        );
    }

    private void blacklistCurrentAccessToken(String accessToken) {
        long remainingMs = jwtProvider.getRemainingExpiration(accessToken);
        if (remainingMs > 0) {
            redisTemplate.opsForValue().set(
                    AuthRedisKeyPrefix.BLACKLIST + accessToken,
                    "1",
                    Duration.ofMillis(remainingMs)
            );
        }
    }

    private long refreshTtlSeconds(String refreshKey) {
        Long ttl = redisTemplate.getExpire(refreshKey, TimeUnit.SECONDS);
        if (ttl == null || ttl < 0) {
            return 0;
        }
        return ttl;
    }

    private SessionKey parseSessionKey(String refreshKey) {
        if (refreshKey == null || !refreshKey.startsWith(AuthRedisKeyPrefix.REFRESH)) {
            return null;
        }

        String[] parts = refreshKey.substring(AuthRedisKeyPrefix.REFRESH.length()).split(":", SESSION_KEY_PART_COUNT);
        if (parts.length != SESSION_KEY_PART_COUNT) {
            return null;
        }

        try {
            return new SessionKey(refreshKey, ClientType.from(parts[1]), parts[2]);
        } catch (BusinessException e) {
            return null;
        }
    }

    private String requireSessionId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new BusinessException(AuthErrorCode.INVALID_ACCESS_TOKEN);
        }
        return sessionId;
    }

    private Map<String, Object> revokeMetadata(ClientType clientType, String sessionId, boolean currentSession) {
        return Map.of(
                "clientType", clientType.name(),
                "sessionId", sessionId,
                "currentSession", currentSession
        );
    }

    private record SessionKey(String refreshKey, ClientType clientType, String sessionId) {
        private boolean isCurrent(ClientType currentClientType, String currentSessionId) {
            return clientType == currentClientType && sessionId.equals(currentSessionId);
        }
    }
}
