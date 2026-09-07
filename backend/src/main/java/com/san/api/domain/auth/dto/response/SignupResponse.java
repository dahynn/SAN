package com.san.api.domain.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.san.api.domain.user.entity.AuthProvider;
import com.san.api.domain.user.entity.User;

import java.time.LocalDateTime;
import java.util.UUID;

/** 회원가입 성공 응답. */
public record SignupResponse(

        UUID userId,
        String username,
        AuthProvider provider,

        // TODO : yaml파일에 시간 통일할 예정
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime createdAt) {
    public static SignupResponse from(User user) {
        return new SignupResponse(
                user.getUserId(),
                user.getUsername(),
                user.getProvider(),
                user.getCreatedAt());
    }
}
