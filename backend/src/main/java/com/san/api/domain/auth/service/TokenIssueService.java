package com.san.api.domain.auth.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.san.api.domain.auth.dto.response.TokenResponse;
import com.san.api.domain.auth.entity.ClientType;
import com.san.api.domain.user.entity.UserRole;
import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.CommonErrorCode;
import com.san.api.global.security.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

/** access token과 refresh token 발급을 담당하는 서비스. */
@Service
@RequiredArgsConstructor
public class TokenIssueService {

    private final JwtProvider jwtProvider;
    private final StringRedisTemplate redisTemplate;
    private final AuthSessionKeyService authSessionKeyService;
    private final RefreshTokenHashService refreshTokenHashService;
    private final ObjectMapper objectMapper;

    @Value("${jwt.access-expiration}")
    private long accessExpiration;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    /**
     * 사용자 ID를 subject로 하는 서비스 JWT 토큰 쌍을 발급합니다.
     *
     * Refresh token은 clientType/sessionId 단위로 분리하고, Redis에는 원문 대신 검증용 메타데이터만 저장합니다.
     */
    public TokenResponse issueTokenPair(String userId, ClientType clientType) {
        return issueTokenPair(userId, clientType, UUID.randomUUID().toString());
    }

    public TokenResponse issueTokenPair(String userId, ClientType clientType, UserRole role) {
        return issueTokenPair(userId, clientType, UUID.randomUUID().toString(), UUID.randomUUID().toString(), role);
    }

    public TokenResponse issueTokenPair(String userId, ClientType clientType, String sessionId) {
        return issueTokenPair(userId, clientType, sessionId, UUID.randomUUID().toString());
    }

    public TokenResponse issueTokenPair(String userId, ClientType clientType, String sessionId, String familyId) {
        return issueTokenPair(userId, clientType, sessionId, familyId, UserRole.USER);
    }

    public TokenResponse issueTokenPair(String userId, ClientType clientType, String sessionId, String familyId,
                                        UserRole role) {
        String accessToken = jwtProvider.generateAccessToken(userId, clientType, sessionId, role);
        String refreshToken = jwtProvider.generateRefreshToken(userId, clientType, sessionId, familyId, role);
        // Redis에는 refresh token 원문을 저장하지 않고 검증용 메타데이터만 저장합니다.
        RefreshTokenSession session = new RefreshTokenSession(
                refreshTokenHashService.hash(refreshToken),
                familyId,
                jwtProvider.getSessionClaims(refreshToken).jti()
        );

        String refreshKey = authSessionKeyService.refreshKey(userId, clientType, sessionId);
        String indexKey = authSessionKeyService.userRefreshIndexKey(userId);

        redisTemplate.opsForValue().set(
                refreshKey,
                serialize(session),
                Duration.ofMillis(refreshExpiration)
        );
        redisTemplate.opsForSet().add(indexKey, refreshKey);
        redisTemplate.expire(indexKey, Duration.ofMillis(refreshExpiration));

        return TokenResponse.of(accessToken, refreshToken, accessExpiration / 1000, sessionId);
    }

    private String serialize(RefreshTokenSession session) {
        try {
            return objectMapper.writeValueAsString(session);
        } catch (JsonProcessingException e) {
            throw new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}
