package com.san.api.domain.til.service;

import com.san.api.domain.til.entity.DailySummary;
import com.san.api.global.external.ai.client.AiTilClient;
import com.san.api.global.external.ai.dto.request.AiTilRequest;
import com.san.api.global.external.ai.dto.response.AiTilResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


/** AI TIL 생성 요청 Service */
@Service
@RequiredArgsConstructor
public class TilGenerationService {

    private final TilSourceService tilSourceService;
    private final AiTilClient aiTilClient;

    /**
     * 등록 시점에 고정한 지식 원본을 기반으로 AI TIL 생성을 요청
     *
     * @param summary 원본 스냅샷을 가진 TIL
     * @return AI TIL 생성 응답
     */
    @Transactional(readOnly = true)
    public AiTilResponse generate(DailySummary summary) {
        return aiTilClient.generateTil(new AiTilRequest(
                tilSourceService.toAiContents(summary),
                true
        ));
    }
}
