package com.san.api.domain.archive.dto.response;

import com.san.api.domain.knowledge.entity.CardTag;

import java.util.UUID;

/** 아카이브 지식카드 태그 응답 DTO */
public record ArchiveCardTagResponse(
        UUID tagId,
        String tagName
) {

    public static ArchiveCardTagResponse from(CardTag cardTag) {
        return new ArchiveCardTagResponse(
                cardTag.getTag().getTagId(),
                cardTag.getTag().getTagName()
        );
    }
}
