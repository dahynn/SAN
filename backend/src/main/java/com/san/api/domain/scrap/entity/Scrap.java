package com.san.api.domain.scrap.entity;

import com.san.api.domain.user.entity.User;
import com.san.api.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** 스크랩 원본 데이터 저장 엔티티 */
@Entity
@Table(name = "scraps")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Scrap extends BaseEntity {

    @Id
    @Column(name = "scrap_id", columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID scrapId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 10)
    private SourceType sourceType;

    @Column(name = "source_url", columnDefinition = "text")
    private String sourceUrl;

    @Column(name = "raw_content", columnDefinition = "text")
    private String rawContent;

    @Column(name = "refined_content", columnDefinition = "text")
    private String refinedContent;

    // 동일 사용자의 중복 원본 저장 방지를 위한 정규화 원본 해시
    @Column(name = "content_hash", length = 64)
    private String contentHash;

    // S3에 저장된 이미지 object key
    @Column(name = "image_object_key", length = 1024)
    private String imageObjectKey;

    @Builder
    public Scrap(
            User user,
            SourceType sourceType,
            String sourceUrl,
            String rawContent,
            String refinedContent,
            String contentHash,
            String imageObjectKey
    ) {
        this.scrapId = UUID.randomUUID();
        this.user = user;
        this.sourceType = sourceType;
        this.sourceUrl = sourceUrl;
        this.rawContent = rawContent;
        this.refinedContent = refinedContent;
        this.contentHash = contentHash;
        this.imageObjectKey = imageObjectKey;
    }

    /**
     * 원본 정제 내용 저장
     *
     * @param refinedContent AI가 정제한 원본 내용
     */
    public void updateRefinedContent(String refinedContent) {
        this.refinedContent = refinedContent;
    }
}
