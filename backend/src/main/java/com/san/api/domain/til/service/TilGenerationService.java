package com.san.api.domain.til.service;

import com.san.api.domain.til.dto.response.TilGenerationSourceContentResponse;
import com.san.api.domain.til.dto.response.TilGenerationSourceResponse;
import com.san.api.global.external.ai.client.AiTilClient;
import com.san.api.global.external.ai.dto.request.AiTilContentRequest;
import com.san.api.global.external.ai.dto.request.AiTilRequest;
import com.san.api.global.external.ai.dto.response.AiTilResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

/** AI TIL 생성 요청 Service */
@Service
@RequiredArgsConstructor
public class TilGenerationService {

    private final TilSourceService tilSourceService;
    private final AiTilClient aiTilClient;

    /**
     * 특정 날짜의 지식 원본을 기반으로 AI TIL 생성을 요청
     *
     * @param userId 로그인 사용자 ID
     * @param targetDate TIL 생성 대상 날짜
     * @return AI TIL 생성 응답
     */
    @Transactional(readOnly = true)
    public AiTilResponse generate(UUID userId, LocalDate targetDate) {
        TilGenerationSourceResponse source = tilSourceService.getSource(userId, targetDate);

        return aiTilClient.generateTil(new AiTilRequest(
                source.contents().stream()
                        .map(this::toAiTilContentRequest)
                        .toList(),
                true
        ));
    }

    /**
     * TIL 원본 DTO를 AI 서버 요청 DTO로 변환
     *
     * @param content TIL 생성용 지식 원본
     * @return AI TIL 생성 요청 원본 DTO
     */
    private AiTilContentRequest toAiTilContentRequest(TilGenerationSourceContentResponse content) {
        return new AiTilContentRequest(content.inputType(), content.content());
    }
}
