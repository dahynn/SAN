package com.san.api.domain.til.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/** TIL 생성 당시 외부 AI 입력 보호 처리 기록입니다. */
public record TilDataProtectionResponse(
        String policyVersion,
        int maskedItemCount,
        boolean transmissionConfirmed,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime transmissionConfirmedAt
) {
}
