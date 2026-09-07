package com.san.api.domain.knowledge.repository;

import com.san.api.domain.knowledge.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** 카테고리 Repository */
public interface CategoryRepository extends JpaRepository<Category, UUID> {

    // 사용자와 카테고리명 기준 카테고리 조회
    Optional<Category> findByUser_UserIdAndCategoryName(UUID userId, String categoryName);

    // 사용자와 카테고리 ID 기준 카테고리 조회
    Optional<Category> findByCategoryIdAndUser_UserId(UUID categoryId, UUID userId);

    /**
     * 아카이브 카테고리 목록과 카테고리별 지식카드 개수를 조회
     *
     * @param userId 사용자 ID
     * @return 지식카드가 있는 카테고리별 카드 개수
     */
    @Query("""
            SELECT c.categoryId AS categoryId,
                   c.categoryName AS categoryName,
                   COUNT(kc) AS cardCount
            FROM KnowledgeCard kc
            JOIN kc.category c
            JOIN kc.scrap s
            WHERE c.user.userId = :userId
              AND s.user.userId = :userId
            GROUP BY c.categoryId, c.categoryName
            """)
    List<CategoryCardCountProjection> findArchiveCategoryCounts(@Param("userId") UUID userId);

    // 카테고리 정보와 집계된 카드 개수를 함께 받기 위한 조회 Projection
    interface CategoryCardCountProjection {
        UUID getCategoryId();

        String getCategoryName();

        long getCardCount();
    }
}
