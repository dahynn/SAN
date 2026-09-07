package com.san.api.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 회원가입 요청 DTO.
 *
 * username: 영문 소문자·숫자 조합, 4~20자
 * password: 영문·숫자·특수문자 조합, 8~20자
 */
public record SignupRequest(

                @NotBlank(message = "아이디를 입력해주세요.") @Size(min = 4, max = 20, message = "아이디는 4~20자 사이여야 합니다.") @Pattern(regexp = "^[a-z0-9]+$", message = "아이디는 영문 소문자와 숫자만 사용할 수 있습니다.") String username,

                @NotBlank(message = "비밀번호를 입력해주세요.") @Size(min = 8, max = 20, message = "비밀번호는 8~20자 사이여야 합니다.") @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).+$", message = "비밀번호는 영문, 숫자, 특수문자를 각각 1자 이상 포함해야 합니다.") String password) {
}
