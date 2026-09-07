package com.san.api.domain.auth.dto.request;

import com.san.api.domain.auth.entity.ClientType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** 일반 로그인 요청 DTO입니다. */
public record LoginRequest(

        @NotBlank(message = "아이디를 입력해주세요.")
        String username,

        @NotBlank(message = "비밀번호를 입력해주세요.")
        String password,

        @NotNull(message = "클라이언트 타입을 입력해주세요.")
        ClientType clientType
) {
}
