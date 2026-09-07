package com.san.api.domain.til.dto.response;

import java.time.LocalDate;

/** TIL GitHub contribution 잔디의 단일 날짜 응답 DTO */
public record TilGithubContributionDayResponse(
        LocalDate date,
        int count,
        int level
) {
}
