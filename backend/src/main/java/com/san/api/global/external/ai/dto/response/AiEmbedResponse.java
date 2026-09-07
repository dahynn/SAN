package com.san.api.global.external.ai.dto.response;

import java.util.List;

/** AI 서버 임베딩 응답 DTO */
public record AiEmbedResponse(List<Float> embedding) {
}
