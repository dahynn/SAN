package com.san.api.domain.knowledge.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

/** 카드 태그 매핑 엔티티 */
@Entity
@Table(name = "card_tags")
@Getter
@IdClass(CardTag.CardTagId.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CardTag {

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "card_id", nullable = false)
    private KnowledgeCard knowledgeCard;

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tag_id", nullable = false)
    private Tag tag;

    public CardTag(KnowledgeCard knowledgeCard, Tag tag) {
        this.knowledgeCard = knowledgeCard;
        this.tag = tag;
    }

    /** 카드 태그 매핑 복합키 */
    @Getter
    @EqualsAndHashCode
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class CardTagId implements Serializable {

        private UUID knowledgeCard;
        private UUID tag;

        public CardTagId(UUID knowledgeCard, UUID tag) {
            this.knowledgeCard = knowledgeCard;
            this.tag = tag;
        }
    }
}
