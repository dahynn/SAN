package com.san.api.global.security.filter;

import com.san.api.domain.auth.service.AuthSessionKeyService;
import com.san.api.global.exception.errorcode.AuthErrorCode;
import com.san.api.global.exception.errorcode.ErrorCode;
import com.san.api.global.security.handler.SecurityErrorAttribute;
import com.san.api.global.security.jwt.JwtProvider;
import com.san.api.global.security.jwt.JwtSessionClaims;
import com.san.api.global.security.redis.AuthRedisKeyPrefix;
import com.san.api.global.security.token.BearerTokenResolver;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Authorization 헤더의 Bearer access token을 검증하고 인증 정보를 등록합니다.
 *
 * <p>토큰이 유효하고 blacklist에 없으면 {@code SecurityContext}에 인증 정보를
 * 저장합니다. 토큰이 잘못되었거나 로그아웃되어 blacklist에 있으면 직접 응답하지
 * 않고 request attribute에 실패 사유만 남깁니다. 최종 오류 응답은
 * {@code CustomAuthenticationEntryPoint}가 공통 형식으로 작성합니다.</p>
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final StringRedisTemplate redisTemplate;
    private final AuthSessionKeyService authSessionKeyService;

    /**
     * 요청마다 한 번 실행되어 Bearer access token을 인증 처리합니다.
     *
     * @param request 현재 HTTP 요청
     * @param response 현재 HTTP 응답
     * @param filterChain 다음 필터 체인
     * @throws ServletException 필터 처리 실패 시
     * @throws IOException 필터 처리 중 입출력 실패 시
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = BearerTokenResolver.resolve(request);

        if (token != null) {
            if (!jwtProvider.validateToken(token) || !jwtProvider.isAccessToken(token)) {
                setSecurityError(request, AuthErrorCode.INVALID_ACCESS_TOKEN);
            } else if (isBlacklisted(token)) {
                setSecurityError(request, AuthErrorCode.TOKEN_BLACKLISTED);
            } else {
                JwtSessionClaims sessionClaims = jwtProvider.getSessionClaims(token);
                if (!hasActiveSession(token, sessionClaims)) {
                    setSecurityError(request, AuthErrorCode.SESSION_REVOKED);
                } else {
                    Authentication auth = jwtProvider.getAuthentication(token);
                    setSessionClaims(auth, sessionClaims);
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 로그아웃 처리된 access token인지 Redis blacklist에서 확인합니다.
     *
     * @param token 검사할 access token
     * @return blacklist에 존재하면 true
     */
    private boolean isBlacklisted(String token) {
        return redisTemplate.opsForValue().get(AuthRedisKeyPrefix.BLACKLIST + token) != null;
    }

    /**
     * Access token의 sessionId가 Redis에 남아있는 refresh 세션과 연결되어 있는지 확인합니다.
     * 세션 폐기 API가 refresh key를 삭제하면, 같은 sessionId의 access token도 다음 요청부터 거부됩니다.
     */
    private boolean hasActiveSession(String token, JwtSessionClaims sessionClaims) {
        String sessionId = sessionClaims.sessionId();
        if (sessionId == null || sessionId.isBlank()) {
            return false;
        }

        String userId = jwtProvider.getUserId(token);
        String refreshKey = authSessionKeyService.refreshKey(userId, sessionClaims.clientType(), sessionId);
        return Boolean.TRUE.equals(redisTemplate.hasKey(refreshKey));
    }

    /** 컨트롤러가 access token을 다시 파싱하지 않도록 인증 details에 세션 클레임을 담습니다. */
    private void setSessionClaims(Authentication authentication, JwtSessionClaims sessionClaims) {
        if (authentication instanceof AbstractAuthenticationToken token) {
            token.setDetails(sessionClaims);
        }
    }

    /**
     * Security Handler가 사용할 인증 실패 사유를 request attribute에 저장합니다.
     *
     * @param request 현재 요청
     * @param errorCode 클라이언트에 내려줄 인증 오류 코드
     */
    private void setSecurityError(HttpServletRequest request, ErrorCode errorCode) {
        request.setAttribute(SecurityErrorAttribute.ERROR_CODE, errorCode);
    }
}
