package com.san.api.domain.til.service;

import com.san.api.domain.knowledge.entity.KnowledgeCard;
import com.san.api.domain.knowledge.repository.KnowledgeCardRepository;
import com.san.api.domain.scrap.entity.Scrap;
import com.san.api.domain.scrap.entity.SourceType;
import com.san.api.domain.til.dto.response.TilGenerationSourceContentResponse;
import com.san.api.domain.til.dto.response.TilGenerationSourceResponse;
import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.TilErrorCode;
import com.san.api.global.external.s3.service.S3PresignedUrlService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** TIL 생성용 지식 원본 구성 Service */
@Service
@RequiredArgsConstructor
public class TilSourceService {

    private final KnowledgeCardRepository knowledgeCardRepository;
    private final S3PresignedUrlService s3PresignedUrlService;

    /**
     * TIL 생성에 사용할 지식카드 정제 원본 목록 구성
     *
     * @param userId 사용자 ID
     * @param targetDate TIL 생성 대상 날짜
     * @return TIL 생성용 지식 정제 원본 목록
     */
    @Transactional(readOnly = true)
    public TilGenerationSourceResponse getSource(UUID userId, LocalDate targetDate) {
        LocalDateTime startAt = targetDate.atStartOfDay();
        LocalDateTime endAt = targetDate.plusDays(1).atStartOfDay();

        List<TilGenerationSourceContentResponse> contents = knowledgeCardRepository.findTilSourceCards(userId, startAt, endAt).stream()
                .map(KnowledgeCard::getScrap)
                .map(this::toSourceContent)
                .toList();

        if (contents.isEmpty()) {
            throw new BusinessException(TilErrorCode.EMPTY_TIL_SOURCE);
        }

        return new TilGenerationSourceResponse(targetDate, contents);
    }

    /**
     * 지식 원본을 AI TIL 생성 요청 형식으로 변환
     *
     * @param scrap 지식카드의 원본 스크랩
     * @return AI TIL 생성 요청에 사용할 원본 DTO
     */
    private TilGenerationSourceContentResponse toSourceContent(Scrap scrap) {
        String refinedContent = scrap.getRefinedContent();
        if (!isBlank(refinedContent)) {
            return new TilGenerationSourceContentResponse("text", refinedContent.trim());
        }

        String content = resolveContent(scrap);
        if (isBlank(content)) {
            throw new BusinessException(TilErrorCode.INVALID_TIL_SOURCE_CONTENT);
        }

        return new TilGenerationSourceContentResponse(toInputType(scrap.getSourceType()), content.trim());
    }

    /**
     * SourceType을 AI input_type 값으로 변환
     *
     * @param sourceType 수집 원본 유형
     * @return AI 서버에서 사용하는 input_type 값
     */
    private String toInputType(SourceType sourceType) {
        return switch (sourceType) {
            case LINK -> "url";
            case TEXT -> "text";
            case IMAGE -> "image";
        };
    }

    /**
     * 원본 유형별 AI 전달 content 선택
     *
     * @param scrap 지식카드의 원본 스크랩
     * @return AI 서버에 전달할 content
     */
    private String resolveContent(Scrap scrap) {
        return switch (scrap.getSourceType()) {
            case LINK -> firstNotBlank(scrap.getSourceUrl(), scrap.getRawContent());
            case TEXT -> scrap.getRawContent();
            case IMAGE -> resolveImageContent(scrap);
        };
    }

    private String resolveImageContent(Scrap scrap) {
        String imageObjectKey = scrap.getImageObjectKey();
        if (isBlank(imageObjectKey)) {
            return scrap.getRawContent();
        }

        // AI 입력용 조회 URL로 변환
        return s3PresignedUrlService.createDownloadPresignedUrl(imageObjectKey);
    }

    /**
     * 비어있지 않은 첫 번째 문자열 반환
     *
     * @param values 후보 문자열 목록
     * @return 첫 번째 유효 문자열 또는 null
     */
    private String firstNotBlank(String... values) {
        for (String value : values) {
            if (!isBlank(value)) {
                return value;
            }
        }
        return null;
    }

    /**
     * 빈 문자열 여부 확인
     *
     * @param value 검증할 문자열
     * @return null, 빈 문자열 또는 공백 문자열 여부
     */
    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
