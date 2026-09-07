package com.san.api.global.security.jwt;

import com.san.api.domain.auth.entity.ClientType;
import com.san.api.domain.user.entity.UserRole;

/** JWT에 담긴 인증 세션 식별 정보를 표현하는 값 객체입니다. */
public record JwtSessionClaims(
        ClientType clientType,
        String sessionId,
        String familyId,
        String jti,
        UserRole role
) {
}
