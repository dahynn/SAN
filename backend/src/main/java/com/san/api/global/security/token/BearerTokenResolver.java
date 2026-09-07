package com.san.api.global.security.token;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

/**
 * HTTP Authorization 헤더에서 Bearer token 값을 추출하는 유틸리티입니다.
 *
 * <p>컨트롤러와 필터에서 같은 방식으로 token을 추출하도록 공통화했습니다.
 * 헤더가 없거나 {@code Bearer } 형식이 아니면 null을 반환합니다.</p>
 */
public final class BearerTokenResolver {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private BearerTokenResolver() {
    }

    public static String resolve(HttpServletRequest request) {
        String bearer = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearer) && bearer.startsWith(BEARER_PREFIX)) {
            return bearer.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
