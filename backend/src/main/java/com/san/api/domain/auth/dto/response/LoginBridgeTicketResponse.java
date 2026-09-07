package com.san.api.domain.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "로그인 브릿지 ticket 발급 응답")
public record LoginBridgeTicketResponse(
        @Schema(description = "Extension 로그인에 사용할 일회용 ticket")
        String ticket,

        @Schema(description = "ticket 만료 시간(초)", example = "120")
        long expiresIn
) {
    public static LoginBridgeTicketResponse of(String ticket, long expiresInSeconds) {
        return new LoginBridgeTicketResponse(ticket, expiresInSeconds);
    }
}
