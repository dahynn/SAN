package com.san.api.domain.til.entity;

import com.san.api.domain.scrap.entity.SourceType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/** TIL 생성 시점에 고정하는 원본 카드 근거 스냅샷입니다. */
@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TilSourceSnapshot {

    @Column(name = "card_id", columnDefinition = "uuid", nullable = false)
    private UUID cardId;

    @Column(name = "scrap_id", columnDefinition = "uuid", nullable = false)
    private UUID scrapId;

    @Column(nullable = false, length = 255)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 10)
    private SourceType sourceType;

    @Column(name = "raw_content", columnDefinition = "text")
    private String rawContent;

    @Column(name = "source_url", columnDefinition = "text")
    private String sourceUrl;

    @Column(name = "image_object_key", length = 1024)
    private String imageObjectKey;

    @Column(name = "category_id", columnDefinition = "uuid")
    private UUID categoryId;

    @Column(name = "category_name", length = 255)
    private String categoryName;

    @Column(name = "source_created_at", nullable = false)
    private LocalDateTime sourceCreatedAt;

    @Column(name = "ai_input_type", nullable = false, length = 10)
    private String aiInputType;

    @Column(name = "ai_input_content", columnDefinition = "text")
    private String aiInputContent;

    public TilSourceSnapshot(
            UUID cardId,
            UUID scrapId,
            String title,
            SourceType sourceType,
            String rawContent,
            String sourceUrl,
            String imageObjectKey,
            UUID categoryId,
            String categoryName,
            LocalDateTime sourceCreatedAt,
            String aiInputType,
            String aiInputContent
    ) {
        this.cardId = cardId;
        this.scrapId = scrapId;
        this.title = title;
        this.sourceType = sourceType;
        this.rawContent = rawContent;
        this.sourceUrl = sourceUrl;
        this.imageObjectKey = imageObjectKey;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.sourceCreatedAt = sourceCreatedAt;
        this.aiInputType = aiInputType;
        this.aiInputContent = aiInputContent;
    }
}
