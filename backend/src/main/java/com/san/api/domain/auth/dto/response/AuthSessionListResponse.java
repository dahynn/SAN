package com.san.api.domain.auth.dto.response;

import com.san.api.domain.auth.entity.ClientType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "인증 세션 목록 응답")
public record AuthSessionListResponse(
        @Schema(description = "사용자의 인증 세션 목록")
        List<Session> sessions
) {
    public static AuthSessionListResponse from(List<Session> sessions) {
        return new AuthSessionListResponse(sessions);
    }

    @Schema(description = "인증 세션 정보")
    public record Session(
            @Schema(description = "인증 세션 식별자", example = "6b2e5e8b-9b95-46cb-a542-7a9ffb0bb89d")
            String sessionId,

            @Schema(description = "세션을 발급받은 클라이언트 유형", example = "DASHBOARD")
            ClientType clientType,

            @Schema(description = "현재 요청에 사용된 access token의 세션 여부", example = "true")
            boolean current,

            @Schema(description = "refresh token 세션의 남은 만료 시간(초)", example = "604800")
            long expiresInSeconds
    ) {
        public static Session of(String sessionId, ClientType clientType, boolean current, long expiresInSeconds) {
            return new Session(sessionId, clientType, current, expiresInSeconds);
        }
    }
}
