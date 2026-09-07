package com.san.api.domain.auth.service;

/** Redis에 저장되는 refresh token 세션 메타데이터입니다. */
public record RefreshTokenSession(
        String tokenHash,
        String familyId,
        String jti
) {
}
