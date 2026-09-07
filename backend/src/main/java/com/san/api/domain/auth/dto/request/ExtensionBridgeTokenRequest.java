package com.san.api.domain.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "익스텐션 로그인 브릿지 토큰 교환 요청")
public record ExtensionBridgeTokenRequest(
        @Schema(description = "대시보드에서 발급받은 익스텐션 로그인용 일회성 브릿지 ticket")
        @NotBlank(message = "익스텐션 로그인 브릿지 ticket을 입력해주세요.")
        String ticket
) {
}
