package com.san.api.domain.user.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AuthProvider {
    LOCAL("자체 회원가입"),
    GITHUB("OAuth2 소셜 로그인");

    private final String description;
}
