package com.san.api.domain.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "대시보드 로그인 브릿지 토큰 교환 요청")
public record DashboardBridgeTokenRequest(
        @Schema(description = "익스텐션에서 발급받은 대시보드 로그인용 일회성 브릿지 ticket")
        @NotBlank(message = "대시보드 로그인 브릿지 ticket을 입력해주세요.")
        String ticket
) {
}
