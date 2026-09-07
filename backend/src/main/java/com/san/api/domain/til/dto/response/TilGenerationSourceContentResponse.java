package com.san.api.domain.til.dto.response;

/** TIL 생성용 지식 원본 단건 응답 DTO */
public record TilGenerationSourceContentResponse(
        String inputType,
        String content
) {
}
