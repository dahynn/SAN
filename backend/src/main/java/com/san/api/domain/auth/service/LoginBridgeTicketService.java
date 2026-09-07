package com.san.api.domain.auth.service;

import com.san.api.domain.auth.dto.response.LoginBridgeTicketResponse;
import com.san.api.domain.auth.entity.ClientType;
import com.san.api.domain.user.entity.UserRole;
import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.AuthErrorCode;
import com.san.api.global.security.jwt.JwtProvider;
import com.san.api.global.security.jwt.JwtSessionClaims;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class LoginBridgeTicketService {

    private static final String TICKET_VALUE_SEPARATOR = "|";

    private final JwtProvider jwtProvider;
    private final StringRedisTemplate redisTemplate;
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * 요청 출처 clientType을 확인한 뒤 짧은 수명의 일회성 bridge ticket을 Redis에 저장합니다.
     */
    public LoginBridgeTicketResponse issueTicket(
            String accessToken,
            ClientType sourceClientType,
            String ticketKeyPrefix,
            Duration ttl) {
        return issueTicketWithContext(accessToken, sourceClientType, ticketKeyPrefix, ttl).response();
    }

    LoginBridgeTicketIssueResult issueTicketWithContext(
            String accessToken,
            ClientType sourceClientType,
            String ticketKeyPrefix,
            Duration ttl) {
        if (!jwtProvider.validateToken(accessToken) || !jwtProvider.isAccessToken(accessToken)) {
            throw new BusinessException(AuthErrorCode.INVALID_ACCESS_TOKEN);
        }

        JwtSessionClaims sessionClaims = jwtProvider.getSessionClaims(accessToken);
        if (sessionClaims.clientType() != sourceClientType) {
            throw new BusinessException(AuthErrorCode.INVALID_ACCESS_TOKEN);
        }

        String userId = jwtProvider.getUserId(accessToken);
        UserRole role = sessionClaims.role();
        String ticket = generateUrlSafeToken();
        redisTemplate.opsForValue().set(
                ticketKeyPrefix + ticket,
                serializeTicketValue(userId, role),
                ttl
        );

        return new LoginBridgeTicketIssueResult(
                userId,
                sourceClientType,
                role,
                LoginBridgeTicketResponse.of(ticket, ttl.toSeconds())
        );
    }

    /**
     * bridge ticket을 한 번만 사용할 수 있도록 Redis에서 조회와 삭제를 동시에 수행합니다.
     */
    public String consumeTicket(String ticket, String ticketKeyPrefix) {
        return consumeTicketWithContext(ticket, ticketKeyPrefix).userId();
    }

    LoginBridgeTicketConsumeResult consumeTicketWithContext(String ticket, String ticketKeyPrefix) {
        String userId = redisTemplate.opsForValue().getAndDelete(ticketKeyPrefix + ticket);
        if (userId == null) {
            throw new BusinessException(AuthErrorCode.INVALID_LOGIN_BRIDGE_TICKET);
        }

        return parseTicketValue(userId);
    }

    private String serializeTicketValue(String userId, UserRole role) {
        return userId + TICKET_VALUE_SEPARATOR + role.name();
    }

    private LoginBridgeTicketConsumeResult parseTicketValue(String value) {
        String[] parts = value.split("\\|", 2);
        if (parts.length == 1) {
            return new LoginBridgeTicketConsumeResult(parts[0], UserRole.USER);
        }
        return new LoginBridgeTicketConsumeResult(parts[0], UserRole.valueOf(parts[1]));
    }

    private String generateUrlSafeToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
