package com.san.api.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

/** 비밀번호 재확인이 필요한 회원탈퇴 요청. */
public record WithdrawRequest(

        @NotBlank(message = "비밀번호를 입력해주세요.")
        String password
) {}
