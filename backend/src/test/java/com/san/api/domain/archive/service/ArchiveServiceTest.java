package com.san.api.domain.archive.service;

import com.san.api.domain.archive.dto.response.ArchiveCardTagRelationResponse;
import com.san.api.domain.archive.dto.response.ArchiveCategoryCardListResponse;
import com.san.api.domain.archive.dto.response.ArchiveCategoryListResponse;
import com.san.api.domain.knowledge.entity.CardTag;
import com.san.api.domain.knowledge.entity.Category;
import com.san.api.domain.knowledge.entity.KnowledgeCard;
import com.san.api.domain.knowledge.entity.Tag;
import com.san.api.domain.knowledge.repository.CardTagRepository;
import com.san.api.domain.knowledge.repository.CardTagRepository.CardTagRelationProjection;
import com.san.api.domain.knowledge.repository.CategoryRepository;
import com.san.api.domain.knowledge.repository.KnowledgeCardRepository;
import com.san.api.domain.scrap.entity.Scrap;
import com.san.api.domain.scrap.entity.SourceType;
import com.san.api.domain.user.entity.AuthProvider;
import com.san.api.domain.user.entity.User;
import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.CommonErrorCode;
import com.san.api.global.exception.errorcode.KnowledgeErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArchiveServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private KnowledgeCardRepository knowledgeCardRepository;

    @Mock
    private CardTagRepository cardTagRepository;

    @InjectMocks
    private ArchiveService archiveService;

    @Test
    void getCategories_returnsCategoryCounts() {
        UUID userId = UUID.randomUUID();
        UUID backendCategoryId = UUID.randomUUID();
        UUID securityCategoryId = UUID.randomUUID();

        when(categoryRepository.findArchiveCategoryCounts(userId))
                .thenReturn(List.of(
                        new TestCategoryCardCountProjection(backendCategoryId, "백엔드", 3),
                        new TestCategoryCardCountProjection(securityCategoryId, "보안", 1)
                ));

        ArchiveCategoryListResponse response = archiveService.getCategories(userId);

        assertThat(response.categories()).hasSize(2);
        assertThat(response.categories()).extracting("categoryId")
                .containsExactly(backendCategoryId, securityCategoryId);
        assertThat(response.categories()).extracting("categoryName")
                .containsExactly("백엔드", "보안");
        assertThat(response.categories()).extracting("cardCount")
                .containsExactly(3L, 1L);
    }

    @Test
    void getCategories_returnsEmptyListWhenNoArchivedCardsExist() {
        UUID userId = UUID.randomUUID();
        when(categoryRepository.findArchiveCategoryCounts(userId)).thenReturn(List.of());

        ArchiveCategoryListResponse response = archiveService.getCategories(userId);

        assertThat(response.categories()).isEmpty();
    }

    @Test
    void getCategoryCards_returnsCardsInSelectedCategory() {
        User user = createUser();
        UUID userId = user.getUserId();
        Category category = createCategory(user, "백엔드");
        KnowledgeCard springCard = createCard(user, category, "Spring 트랜잭션");
        KnowledgeCard jpaCard = createCard(user, category, "JPA 연관관계");
        Tag springTag = createTag("Spring");
        Tag jpaTag = createTag("JPA");

        when(categoryRepository.findByCategoryIdAndUser_UserId(category.getCategoryId(), userId))
                .thenReturn(Optional.of(category));
        when(knowledgeCardRepository.findArchiveCardsByCategory(userId, category.getCategoryId()))
                .thenReturn(List.of(springCard, jpaCard));
        when(cardTagRepository.findAllByKnowledgeCardInWithTag(List.of(springCard, jpaCard)))
                .thenReturn(List.of(
                        new CardTag(springCard, springTag),
                        new CardTag(jpaCard, jpaTag)
                ));

        ArchiveCategoryCardListResponse response = archiveService.getCategoryCards(userId, category.getCategoryId());

        assertThat(response.categoryId()).isEqualTo(category.getCategoryId());
        assertThat(response.categoryName()).isEqualTo("백엔드");
        assertThat(response.cards()).hasSize(2);
        assertThat(response.cards()).extracting("cardId")
                .containsExactly(springCard.getCardId(), jpaCard.getCardId());
        assertThat(response.cards()).extracting("title")
                .containsExactly("Spring 트랜잭션", "JPA 연관관계");
        assertThat(response.cards().get(0).tags()).extracting("tagName")
                .containsExactly("Spring");
        assertThat(response.cards().get(1).tags()).extracting("tagName")
                .containsExactly("JPA");
    }

    @Test
    void getCategoryCards_returnsEmptyCardsWhenCategoryHasNoCards() {
        User user = createUser();
        UUID userId = user.getUserId();
        Category category = createCategory(user, "백엔드");

        when(categoryRepository.findByCategoryIdAndUser_UserId(category.getCategoryId(), userId))
                .thenReturn(Optional.of(category));
        when(knowledgeCardRepository.findArchiveCardsByCategory(userId, category.getCategoryId()))
                .thenReturn(List.of());

        ArchiveCategoryCardListResponse response = archiveService.getCategoryCards(userId, category.getCategoryId());

        assertThat(response.categoryId()).isEqualTo(category.getCategoryId());
        assertThat(response.categoryName()).isEqualTo("백엔드");
        assertThat(response.cards()).isEmpty();
        verify(cardTagRepository, never()).findAllByKnowledgeCardInWithTag(List.of());
    }

    @Test
    void getCategoryCards_throwsExceptionWhenCategoryNotFound() {
        UUID userId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();

        when(categoryRepository.findByCategoryIdAndUser_UserId(categoryId, userId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> archiveService.getCategoryCards(userId, categoryId))
                .isInstanceOf(BusinessException.class)
                .hasMessage(CommonErrorCode.RESOURCE_NOT_FOUND.getMessage());
    }

    @Test
    void getCardTagRelations_returnsRelatedCardsWithMatchedTags() {
        User user = createUser();
        UUID userId = user.getUserId();
        Category backendCategory = createCategory(user, "백엔드");
        Category securityCategory = createCategory(user, "보안");
        KnowledgeCard selectedCard = createCard(user, backendCategory, "JWT 인증");
        Tag jwtTag = createTag("JWT");
        Tag springTag = createTag("Spring");
        UUID relatedCardId = UUID.randomUUID();
        UUID securityCardId = UUID.randomUUID();

        when(knowledgeCardRepository.findByCardIdWithScrapAndCategory(selectedCard.getCardId()))
                .thenReturn(Optional.of(selectedCard));
        when(cardTagRepository.findAllByCardIdWithTag(selectedCard.getCardId()))
                .thenReturn(List.of(
                        new CardTag(selectedCard, jwtTag),
                        new CardTag(selectedCard, springTag)
                ));
        when(cardTagRepository.findRelatedCardTagRelations(
                userId,
                selectedCard.getCardId(),
                List.of(jwtTag.getTagId(), springTag.getTagId())
        )).thenReturn(List.of(
                new TestCardTagRelationProjection(
                        relatedCardId,
                        backendCategory.getCategoryId(),
                        "백엔드",
                        "Spring Security",
                        jwtTag.getTagId(),
                        "JWT",
                        2
                ),
                new TestCardTagRelationProjection(
                        relatedCardId,
                        backendCategory.getCategoryId(),
                        "백엔드",
                        "Spring Security",
                        springTag.getTagId(),
                        "Spring",
                        2
                ),
                new TestCardTagRelationProjection(
                        securityCardId,
                        securityCategory.getCategoryId(),
                        "보안",
                        "토큰 보안",
                        jwtTag.getTagId(),
                        "JWT",
                        1
                )
        ));

        ArchiveCardTagRelationResponse response = archiveService.getCardTagRelations(userId, selectedCard.getCardId());

        assertThat(response.selectedCardId()).isEqualTo(selectedCard.getCardId());
        assertThat(response.relatedCards()).hasSize(2);
        assertThat(response.relatedCards()).extracting("cardId")
                .containsExactly(relatedCardId, securityCardId);
        assertThat(response.relatedCards()).extracting("matchedTagCount")
                .containsExactly(2L, 1L);
        assertThat(response.relatedCards().get(0).matchedTags()).extracting("tagName")
                .containsExactly("JWT", "Spring");
        assertThat(response.relatedCards().get(1).matchedTags()).extracting("tagName")
                .containsExactly("JWT");
    }

    @Test
    void getCardTagRelations_returnsEmptyRelatedCardsWhenSelectedCardHasNoTags() {
        User user = createUser();
        UUID userId = user.getUserId();
        Category category = createCategory(user, "백엔드");
        KnowledgeCard selectedCard = createCard(user, category, "태그 없는 카드");

        when(knowledgeCardRepository.findByCardIdWithScrapAndCategory(selectedCard.getCardId()))
                .thenReturn(Optional.of(selectedCard));
        when(cardTagRepository.findAllByCardIdWithTag(selectedCard.getCardId()))
                .thenReturn(List.of());

        ArchiveCardTagRelationResponse response = archiveService.getCardTagRelations(userId, selectedCard.getCardId());

        assertThat(response.selectedCardId()).isEqualTo(selectedCard.getCardId());
        assertThat(response.relatedCards()).isEmpty();
        verify(cardTagRepository, never()).findRelatedCardTagRelations(any(), any(), anyList());
    }

    @Test
    void getCardTagRelations_throwsExceptionWhenSelectedCardNotFound() {
        UUID userId = UUID.randomUUID();
        UUID cardId = UUID.randomUUID();

        when(knowledgeCardRepository.findByCardIdWithScrapAndCategory(cardId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> archiveService.getCardTagRelations(userId, cardId))
                .isInstanceOf(BusinessException.class)
                .hasMessage(KnowledgeErrorCode.CARD_NOT_FOUND.getMessage());
    }

    @Test
    void getCardTagRelations_throwsExceptionWhenSelectedCardOwnedByOtherUser() {
        User owner = createUser();
        User otherUser = createUser();
        Category category = createCategory(owner, "백엔드");
        KnowledgeCard selectedCard = createCard(owner, category, "다른 사용자 카드");

        when(knowledgeCardRepository.findByCardIdWithScrapAndCategory(selectedCard.getCardId()))
                .thenReturn(Optional.of(selectedCard));

        assertThatThrownBy(() -> archiveService.getCardTagRelations(otherUser.getUserId(), selectedCard.getCardId()))
                .isInstanceOf(BusinessException.class)
                .hasMessage(KnowledgeErrorCode.CARD_ACCESS_DENIED.getMessage());
    }

    private record TestCategoryCardCountProjection(
            UUID categoryId,
            String categoryName,
            long cardCount
    ) implements CategoryRepository.CategoryCardCountProjection {

        @Override
        public UUID getCategoryId() {
            return categoryId;
        }

        @Override
        public String getCategoryName() {
            return categoryName;
        }

        @Override
        public long getCardCount() {
            return cardCount;
        }
    }

    private record TestCardTagRelationProjection(
            UUID cardId,
            UUID categoryId,
            String categoryName,
            String title,
            UUID tagId,
            String tagName,
            long matchedTagCount
    ) implements CardTagRelationProjection {

        @Override
        public UUID getCardId() {
            return cardId;
        }

        @Override
        public UUID getCategoryId() {
            return categoryId;
        }

        @Override
        public String getCategoryName() {
            return categoryName;
        }

        @Override
        public String getTitle() {
            return title;
        }

        @Override
        public UUID getTagId() {
            return tagId;
        }

        @Override
        public String getTagName() {
            return tagName;
        }

        @Override
        public long getMatchedTagCount() {
            return matchedTagCount;
        }
    }

    private User createUser() {
        return User.builder()
                .username("archive-user")
                .passwordHash("password")
                .provider(AuthProvider.LOCAL)
                .build();
    }

    private Category createCategory(User user, String categoryName) {
        return Category.builder()
                .user(user)
                .categoryName(categoryName)
                .build();
    }

    private KnowledgeCard createCard(User user, Category category, String title) {
        return KnowledgeCard.builder()
                .scrap(createScrap(user))
                .category(category)
                .title(title)
                .summary(title + " 요약")
                .build();
    }

    private Scrap createScrap(User user) {
        return Scrap.builder()
                .user(user)
                .sourceType(SourceType.TEXT)
                .rawContent("원본")
                .refinedContent("정제")
                .build();
    }

    private Tag createTag(String tagName) {
        return Tag.builder()
                .tagName(tagName)
                .build();
    }
}
