package com.san.api.domain.auth.dto.response;

/** 토큰 발급 응답. accessToken만 응답 바디에 포함하고, refreshToken은 별도 처리. */
public record TokenResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        String sessionId
) {
    public static TokenResponse of(String accessToken, String refreshToken, long expiresInSeconds, String sessionId) {
        return new TokenResponse(accessToken, refreshToken, "Bearer", expiresInSeconds, sessionId);
    }
}
