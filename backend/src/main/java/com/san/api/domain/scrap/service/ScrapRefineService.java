package com.san.api.domain.scrap.service;

import com.san.api.domain.scrap.entity.Scrap;
import com.san.api.domain.scrap.entity.SourceType;
import com.san.api.domain.scrap.repository.ScrapRepository;
import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.CommonErrorCode;
import com.san.api.global.external.ai.client.AiScrapRefineClient;
import com.san.api.global.external.ai.dto.request.AiScrapRefineRequest;
import com.san.api.global.external.ai.dto.response.AiScrapRefineResponse;
import com.san.api.global.external.s3.service.S3PresignedUrlService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** 수집 원본 정제 Service */
@Service
@RequiredArgsConstructor
public class ScrapRefineService {

    private final ScrapRepository scrapRepository;
    private final AiScrapRefineClient aiScrapRefineClient;
    private final S3PresignedUrlService s3PresignedUrlService;

    /**
     * 수집 원본을 AI 서버로 정제하고 저장
     *
     * @param scrapId 정제 대상 Scrap ID
     */
    @Transactional
    public void refine(UUID scrapId) {
        Scrap scrap = scrapRepository.findById(scrapId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND));

        AiScrapRefineResponse response = aiScrapRefineClient.refine(new AiScrapRefineRequest(
                toInputType(scrap.getSourceType()),
                resolveContent(scrap)
        ));
        scrap.updateRefinedContent(response.refinedContent());
    }

    /**
     * SourceType을 AI input_type 값으로 변환
     *
     * @param sourceType 수집 원본 타입
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
     * AI 원본 정제 요청에 사용할 content 조회
     *
     * @param scrap 수집 원본
     * @return AI 서버에 전달할 content
     */
    private String resolveContent(Scrap scrap) {
        String content = switch (scrap.getSourceType()) {
            case LINK -> firstNotBlank(scrap.getSourceUrl(), scrap.getRawContent());
            case TEXT -> scrap.getRawContent();
            case IMAGE -> resolveImageContent(scrap);
        };

        if (isBlank(content)) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT_VALUE);
        }

        return content.trim();
    }

    private String resolveImageContent(Scrap scrap) {
        String imageObjectKey = scrap.getImageObjectKey();
        if (isBlank(imageObjectKey)) {
            return scrap.getRawContent();
        }

        return s3PresignedUrlService.createDownloadPresignedUrl(imageObjectKey);
    }

    /**
     * 빈 문자열이 아닌 첫 번째 값 조회
     *
     * @param values 검사할 문자열 목록
     * @return 빈 문자열이 아닌 첫 번째 문자열 또는 null
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
     * 빈 값 여부 확인
     *
     * @param value 확인할 값
     * @return null, 빈 문자열, 공백 문자열 여부
     */
    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
