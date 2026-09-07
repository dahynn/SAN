package com.san.api.domain.knowledge.repository;

import com.san.api.domain.knowledge.entity.CardTag;
import com.san.api.domain.knowledge.entity.CardTag.CardTagId;
import com.san.api.domain.knowledge.entity.KnowledgeCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/** 카드 태그 매핑 Repository */
public interface CardTagRepository extends JpaRepository<CardTag, CardTagId> {

    /**
     * 지식카드 목록 기준 태그 매핑 조회
     *
     * @param cards 지식카드 목록
     * @return 태그 매핑 목록
     */
    @Query("""
            SELECT ct
            FROM CardTag ct
            JOIN FETCH ct.tag
            WHERE ct.knowledgeCard IN :cards
            """)
    List<CardTag> findAllByKnowledgeCardInWithTag(List<KnowledgeCard> cards);

    /**
     * 지식카드 ID 기준 태그 매핑 조회
     *
     * @param cardId 지식카드 ID
     * @return 태그가 포함된 카드 태그 매핑 목록
     */
    @Query("""
            SELECT ct
            FROM CardTag ct
            JOIN FETCH ct.tag
            WHERE ct.knowledgeCard.cardId = :cardId
            """)
    List<CardTag> findAllByCardIdWithTag(@Param("cardId") UUID cardId);

    /**
     * 선택 카드 태그와 겹치는 사용자 지식카드 태그 조회
     *
     * @param userId 사용자 ID
     * @param selectedCardId 선택 카드 ID
     * @param tagIds 선택 카드 태그 ID 목록
     * @return 태그가 겹치는 카드와 매칭 태그 행 목록
     */
    @Query("""
            SELECT kc.cardId AS cardId,
                   c.categoryId AS categoryId,
                   c.categoryName AS categoryName,
                   kc.title AS title,
                   t.tagId AS tagId,
                   t.tagName AS tagName,
                   (
                       SELECT COUNT(ct2)
                       FROM CardTag ct2
                       WHERE ct2.knowledgeCard = kc
                         AND ct2.tag.tagId IN :tagIds
                   ) AS matchedTagCount
            FROM CardTag ct
            JOIN ct.knowledgeCard kc
            JOIN kc.scrap s
            JOIN kc.category c
            JOIN ct.tag t
            WHERE s.user.userId = :userId
              AND kc.cardId <> :selectedCardId
              AND t.tagId IN :tagIds
            ORDER BY kc.updatedAt DESC, kc.createdAt DESC
            """)
    List<CardTagRelationProjection> findRelatedCardTagRelations(
            @Param("userId") UUID userId,
            @Param("selectedCardId") UUID selectedCardId,
            @Param("tagIds") List<UUID> tagIds
    );

    // 태그 연관도 응답 생성을 위한 카드와 매칭 태그 조회 Projection
    interface CardTagRelationProjection {
        UUID getCardId();

        UUID getCategoryId();

        String getCategoryName();

        String getTitle();

        UUID getTagId();

        String getTagName();

        long getMatchedTagCount();
    }
}
