package com.san.api.domain.auth.service;

import com.san.api.domain.auth.dto.response.LoginBridgeTicketResponse;
import com.san.api.domain.auth.entity.ClientType;
import com.san.api.domain.user.entity.UserRole;
import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.AuthErrorCode;
import com.san.api.global.security.jwt.JwtProvider;
import com.san.api.global.security.jwt.JwtSessionClaims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginBridgeTicketServiceTest {

    private static final String TICKET_KEY_PREFIX = "auth:bridge:test:ticket:";

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private LoginBridgeTicketService loginBridgeTicketService;

    @BeforeEach
    void setUp() {
        loginBridgeTicketService = new LoginBridgeTicketService(jwtProvider, redisTemplate);
    }

    @Test
    void issueTicketStoresUserIdForExpectedClientType() {
        String userId = UUID.randomUUID().toString();
        String accessToken = "access-token";
        Duration ttl = Duration.ofSeconds(30);

        when(jwtProvider.validateToken(accessToken)).thenReturn(true);
        when(jwtProvider.isAccessToken(accessToken)).thenReturn(true);
        when(jwtProvider.getSessionClaims(accessToken))
                .thenReturn(new JwtSessionClaims(ClientType.EXTENSION, "extension-session", null, null, UserRole.ADMIN));
        when(jwtProvider.getUserId(accessToken)).thenReturn(userId);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        LoginBridgeTicketResponse response = loginBridgeTicketService.issueTicket(
                accessToken,
                ClientType.EXTENSION,
                TICKET_KEY_PREFIX,
                ttl
        );

        assertThat(response.ticket()).isNotBlank();
        assertThat(response.expiresIn()).isEqualTo(30L);
        verify(valueOperations).set(TICKET_KEY_PREFIX + response.ticket(), userId + "|ADMIN", ttl);
    }

    @Test
    void issueTicketRejectsUnexpectedClientType() {
        String accessToken = "access-token";

        when(jwtProvider.validateToken(accessToken)).thenReturn(true);
        when(jwtProvider.isAccessToken(accessToken)).thenReturn(true);
        when(jwtProvider.getSessionClaims(accessToken))
                .thenReturn(new JwtSessionClaims(ClientType.DASHBOARD, "dashboard-session", null, null, UserRole.USER));

        assertThatThrownBy(() -> loginBridgeTicketService.issueTicket(
                accessToken,
                ClientType.EXTENSION,
                TICKET_KEY_PREFIX,
                Duration.ofSeconds(30)
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(AuthErrorCode.INVALID_ACCESS_TOKEN.getMessage());
    }

    @Test
    void consumeTicketReturnsDeletedUserId() {
        String userId = UUID.randomUUID().toString();
        String ticket = "bridge-ticket";

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.getAndDelete(TICKET_KEY_PREFIX + ticket)).thenReturn(userId + "|ADMIN");

        String result = loginBridgeTicketService.consumeTicket(ticket, TICKET_KEY_PREFIX);

        assertThat(result).isEqualTo(userId);
    }

    @Test
    void consumeTicketRejectsMissingTicket() {
        String ticket = "missing-ticket";

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.getAndDelete(TICKET_KEY_PREFIX + ticket)).thenReturn(null);

        assertThatThrownBy(() -> loginBridgeTicketService.consumeTicket(ticket, TICKET_KEY_PREFIX))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(AuthErrorCode.INVALID_LOGIN_BRIDGE_TICKET.getMessage());
    }
}
