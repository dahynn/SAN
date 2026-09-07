package com.san.api.domain.knowledge.entity;

import com.san.api.domain.scrap.entity.Scrap;
import com.san.api.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Array;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

/** 지식 카드 엔티티 */
@Entity
@Table(
        name = "knowledge_cards",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_knowledge_cards_scrap_id", columnNames = "scrap_id")
        }
)
@SQLRestriction("is_deleted = false")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class KnowledgeCard extends BaseEntity {

    @Id
    @Column(name = "card_id", columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID cardId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scrap_id", nullable = false)
    private Scrap scrap;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "text")
    private String summary;

    @JdbcTypeCode(SqlTypes.VECTOR)
    @Array(length = 1536)
    @Column(columnDefinition = "vector(1536)")
    private float[] embedding;

    @Builder
    public KnowledgeCard(
            Scrap scrap,
            Category category,
            String title,
            String summary,
            float[] embedding
    ) {
        this.cardId = UUID.randomUUID();
        this.scrap = scrap;
        this.category = category;
        this.title = title;
        this.summary = summary;
        this.embedding = embedding;
    }

    /** 지식카드 삭제 처리 */
    public void deleteCard() {
        delete();
    }
}
