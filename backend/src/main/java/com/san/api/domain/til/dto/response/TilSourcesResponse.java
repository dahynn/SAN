package com.san.api.domain.til.dto.response;

import java.util.List;

/** TIL 생성 원본 목록 응답 DTO */
public record TilSourcesResponse(
        List<TilSourceContentResponse> sources
) {
}
