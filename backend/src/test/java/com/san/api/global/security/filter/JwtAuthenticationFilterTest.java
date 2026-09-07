package com.san.api.global.security.filter;

import com.san.api.domain.auth.entity.ClientType;
import com.san.api.domain.auth.service.AuthSessionKeyService;
import com.san.api.domain.user.entity.UserRole;
import com.san.api.global.exception.errorcode.AuthErrorCode;
import com.san.api.global.security.handler.SecurityErrorAttribute;
import com.san.api.global.security.jwt.JwtProvider;
import com.san.api.global.security.jwt.JwtSessionClaims;
import com.san.api.global.security.redis.AuthRedisKeyPrefix;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * JwtAuthenticationFilter가 access token 자체의 유효성뿐 아니라
 * Redis에 남아있는 인증 세션 여부까지 확인하는지 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private AuthSessionKeyService authSessionKeyService;
    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        authSessionKeyService = new AuthSessionKeyService();
        filter = new JwtAuthenticationFilter(jwtProvider, redisTemplate, authSessionKeyService);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void authenticatesAccessTokenWhenRefreshSessionExists() throws Exception {
        String token = "access-token";
        String userId = "user-id";
        String sessionId = "session-id";
        String refreshKey = AuthRedisKeyPrefix.REFRESH + userId + ":DASHBOARD:" + sessionId;
        MockHttpServletRequest request = bearerRequest(token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userId, null, List.of());

        when(jwtProvider.validateToken(token)).thenReturn(true);
        when(jwtProvider.isAccessToken(token)).thenReturn(true);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(AuthRedisKeyPrefix.BLACKLIST + token)).thenReturn(null);
        JwtSessionClaims sessionClaims = new JwtSessionClaims(ClientType.DASHBOARD, sessionId, null, null, UserRole.USER);
        when(jwtProvider.getSessionClaims(token))
                .thenReturn(sessionClaims);
        when(jwtProvider.getUserId(token)).thenReturn(userId);
        when(redisTemplate.hasKey(refreshKey)).thenReturn(true);
        when(jwtProvider.getAuthentication(token)).thenReturn(authentication);

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
        });

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isEqualTo(authentication);
        assertThat(SecurityContextHolder.getContext().getAuthentication().getDetails()).isEqualTo(sessionClaims);
        assertThat(request.getAttribute(SecurityErrorAttribute.ERROR_CODE)).isNull();
    }

    @Test
    void rejectsAccessTokenWhenRefreshSessionWasRevoked() throws Exception {
        String token = "access-token";
        String userId = "user-id";
        String sessionId = "revoked-session";
        String refreshKey = AuthRedisKeyPrefix.REFRESH + userId + ":EXTENSION:" + sessionId;
        MockHttpServletRequest request = bearerRequest(token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtProvider.validateToken(token)).thenReturn(true);
        when(jwtProvider.isAccessToken(token)).thenReturn(true);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(AuthRedisKeyPrefix.BLACKLIST + token)).thenReturn(null);
        when(jwtProvider.getSessionClaims(token))
                .thenReturn(new JwtSessionClaims(ClientType.EXTENSION, sessionId, null, null, UserRole.USER));
        when(jwtProvider.getUserId(token)).thenReturn(userId);
        when(redisTemplate.hasKey(refreshKey)).thenReturn(false);

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
        });

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(request.getAttribute(SecurityErrorAttribute.ERROR_CODE))
                .isEqualTo(AuthErrorCode.SESSION_REVOKED);
        verify(jwtProvider, never()).getAuthentication(token);
    }

    private MockHttpServletRequest bearerRequest(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
        request.addHeader("Authorization", "Bearer " + token);
        return request;
    }
}
