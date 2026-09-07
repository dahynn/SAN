package com.san.api.domain.github.dto.request;

import com.san.api.domain.auth.entity.ClientType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** GitHub authorization code 로그인 요청 DTO입니다. */
public record GithubLoginRequest(

        @NotBlank(message = "GitHub 인증 코드를 입력해주세요.")
        String code,

        @NotNull(message = "클라이언트 타입을 입력해주세요.")
        ClientType clientType
) {
}
