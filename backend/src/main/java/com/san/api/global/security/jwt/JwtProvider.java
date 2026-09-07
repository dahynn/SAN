package com.san.api.global.security.jwt;

import com.san.api.domain.auth.entity.ClientType;
import com.san.api.domain.user.entity.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/** JWT 토큰 생성, 파싱, 검증을 담당합니다. subject는 userId(UUID 문자열)입니다. */
@Component
public class JwtProvider {

    private static final String TOKEN_TYPE_CLAIM = "typ";
    private static final String CLIENT_TYPE_CLAIM = "clientType";
    private static final String SESSION_ID_CLAIM = "sessionId";
    private static final String FAMILY_ID_CLAIM = "familyId";
    private static final String ROLE_CLAIM = "role";
    private static final String ACCESS_TOKEN_TYPE = "access";
    private static final String REFRESH_TOKEN_TYPE = "refresh";

    private final SecretKey secretKey;
    private final long accessExpiration;
    private final long refreshExpiration;

    public JwtProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-expiration}") long accessExpiration,
            @Value("${jwt.refresh-expiration}") long refreshExpiration) {
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.accessExpiration = accessExpiration;
        this.refreshExpiration = refreshExpiration;
    }

    public String generateAccessToken(String userId) {
        return buildToken(userId, accessExpiration, ACCESS_TOKEN_TYPE);
    }

    public String generateAccessToken(String userId, ClientType clientType, String sessionId) {
        return generateAccessToken(userId, clientType, sessionId, UserRole.USER);
    }

    public String generateAccessToken(String userId, ClientType clientType, String sessionId, UserRole role) {
        return buildToken(userId, accessExpiration, ACCESS_TOKEN_TYPE, clientType, sessionId, null, role);
    }

    public String generateRefreshToken(String userId) {
        return buildToken(userId, refreshExpiration, REFRESH_TOKEN_TYPE);
    }

    public String generateRefreshToken(String userId, ClientType clientType, String sessionId) {
        return generateRefreshToken(userId, clientType, sessionId, UUID.randomUUID().toString());
    }

    public String generateRefreshToken(String userId, ClientType clientType, String sessionId, String familyId) {
        return generateRefreshToken(userId, clientType, sessionId, familyId, UserRole.USER);
    }

    public String generateRefreshToken(String userId, ClientType clientType, String sessionId, String familyId,
                                       UserRole role) {
        return buildToken(userId, refreshExpiration, REFRESH_TOKEN_TYPE, clientType, sessionId, familyId, role);
    }

    /** 토큰에서 인증 객체를 추출합니다. principal은 userId(String)입니다. */
    public Authentication getAuthentication(String token) {
        Claims claims = getClaims(token);
        String userId = claims.getSubject();
        UserRole role = getRole(claims);
        return new UsernamePasswordAuthenticationToken(
                userId,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))
        );
    }

    public String getUserId(String token) {
        return getClaims(token).getSubject();
    }

    public JwtSessionClaims getSessionClaims(String token) {
        Claims claims = getClaims(token);
        ClientType clientType = ClientType.from(claims.get(CLIENT_TYPE_CLAIM, String.class));
        String sessionId = claims.get(SESSION_ID_CLAIM, String.class);
        String familyId = claims.get(FAMILY_ID_CLAIM, String.class);
        String jti = claims.getId();
        UserRole role = getRole(claims);
        return new JwtSessionClaims(clientType, sessionId, familyId, jti, role);
    }

    /** 토큰 남은 유효시간(ms)입니다. 블랙리스트 TTL 계산에 사용합니다. */
    public long getRemainingExpiration(String token) {
        Date expiration = getClaims(token).getExpiration();
        return Math.max(0, expiration.getTime() - System.currentTimeMillis());
    }

    public boolean isAccessToken(String token) {
        return ACCESS_TOKEN_TYPE.equals(getTokenType(token));
    }

    public boolean isRefreshToken(String token) {
        return REFRESH_TOKEN_TYPE.equals(getTokenType(token));
    }

    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private String getTokenType(String token) {
        return getClaims(token).get(TOKEN_TYPE_CLAIM, String.class);
    }

    private String buildToken(String userId, long expiration, String tokenType) {
        return buildToken(userId, expiration, tokenType, null, null, null, UserRole.USER);
    }

    private String buildToken(String userId, long expiration, String tokenType, ClientType clientType, String sessionId,
                              String familyId, UserRole role) {
        Date now = new Date();
        var builder = Jwts.builder()
                .subject(userId)
                .id(UUID.randomUUID().toString())
                .claim(TOKEN_TYPE_CLAIM, tokenType)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expiration))
                .signWith(secretKey);

        if (clientType != null) {
            builder.claim(CLIENT_TYPE_CLAIM, clientType.name());
        }
        if (sessionId != null && !sessionId.isBlank()) {
            builder.claim(SESSION_ID_CLAIM, sessionId);
        }
        if (familyId != null && !familyId.isBlank()) {
            builder.claim(FAMILY_ID_CLAIM, familyId);
        }
        if (role != null) {
            builder.claim(ROLE_CLAIM, role.name());
        }

        return builder.compact();
    }

    private UserRole getRole(Claims claims) {
        String role = claims.get(ROLE_CLAIM, String.class);
        if (role == null || role.isBlank()) {
            return UserRole.USER;
        }
        return UserRole.valueOf(role);
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
