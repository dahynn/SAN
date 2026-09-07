package com.san.api.domain.til.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** TIL 수정 요청 DTO */
public record TilUpdateRequest(
        @NotBlank
        @Size(max = 255)
        String title,

        @NotBlank
        @Size(max = 20000)
        String content
) {
}
