package com.san.api.domain.scrap.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.san.api.domain.scrap.entity.Scrap;
import com.san.api.domain.scrap.entity.ScrapCardCreationStatus;
import com.san.api.domain.scrap.entity.ScrapOriginStatus;
import com.san.api.domain.scrap.entity.ScrapRefineStatus;
import com.san.api.domain.scrap.entity.SourceType;

import java.time.LocalDateTime;
import java.util.UUID;

/** 스크랩 저장 및 조회 응답 DTO */
public record ScrapResponse(

        UUID scrapId,
        UUID analysisJobId,
        UUID refineJobId,
        UUID cardId,
        ScrapOriginStatus originStatus,
        ScrapRefineStatus refineStatus,
        ScrapCardCreationStatus cardCreationStatus,
        SourceType sourceType,
        String sourceUrl,
        String rawContent,
        String imageObjectKey,

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime createdAt
) {
    public static ScrapResponse from(Scrap scrap, UUID analysisJobId, UUID cardId) {
        return from(
                scrap,
                analysisJobId,
                null,
                cardId,
                ScrapOriginStatus.CREATED,
                ScrapRefineStatus.REFINE_IN_PROGRESS,
                ScrapCardCreationStatus.ANALYSIS_IN_PROGRESS
        );
    }

    public static ScrapResponse from(
            Scrap scrap,
            UUID analysisJobId,
            UUID refineJobId,
            UUID cardId,
            ScrapOriginStatus originStatus,
            ScrapRefineStatus refineStatus,
            ScrapCardCreationStatus cardCreationStatus
    ) {
        return new ScrapResponse(
                scrap.getScrapId(),
                analysisJobId,
                refineJobId,
                cardId,
                originStatus,
                refineStatus,
                cardCreationStatus,
                scrap.getSourceType(),
                scrap.getSourceUrl(),
                scrap.getRawContent(),
                scrap.getImageObjectKey(),
                scrap.getCreatedAt()
        );
    }
}
