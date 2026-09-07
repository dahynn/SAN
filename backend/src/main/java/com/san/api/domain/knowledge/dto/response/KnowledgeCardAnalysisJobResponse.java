package com.san.api.domain.knowledge.dto.response;

import java.util.UUID;

/** 지식카드 AI 분석 작업 등록 응답 DTO */
public record KnowledgeCardAnalysisJobResponse(
        UUID jobId
) {
}
