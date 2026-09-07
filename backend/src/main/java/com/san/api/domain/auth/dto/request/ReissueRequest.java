package com.san.api.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

/** Access Token 재발급 요청. */
public record ReissueRequest(

        @NotBlank(message = "리프레시 토큰을 입력해주세요.")
        String refreshToken
) {}
