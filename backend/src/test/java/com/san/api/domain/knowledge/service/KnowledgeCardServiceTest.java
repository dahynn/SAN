package com.san.api.domain.knowledge.service;

import com.san.api.domain.knowledge.dto.request.RefinedContentUpdateRequest;
import com.san.api.domain.knowledge.dto.response.KnowledgeCardDetailResponse;
import com.san.api.domain.knowledge.entity.CardTag;
import com.san.api.domain.knowledge.entity.Category;
import com.san.api.domain.knowledge.entity.KnowledgeCard;
import com.san.api.domain.knowledge.entity.Tag;
import com.san.api.domain.knowledge.repository.CardTagRepository;
import com.san.api.domain.knowledge.repository.KnowledgeCardRepository;
import com.san.api.domain.scrap.entity.Scrap;
import com.san.api.domain.scrap.entity.SourceType;
import com.san.api.domain.scrap.repository.ScrapRepository;
import com.san.api.domain.user.entity.AuthProvider;
import com.san.api.domain.user.entity.User;
import com.san.api.global.async.service.AsyncJobManager;
import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.KnowledgeErrorCode;
import com.san.api.global.exception.errorcode.ScrapErrorCode;
import com.san.api.global.external.s3.service.S3PresignedUrlService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KnowledgeCardServiceTest {

    @Mock
    private ScrapRepository scrapRepository;
    @Mock
    private KnowledgeCardRepository knowledgeCardRepository;
    @Mock
    private CardTagRepository cardTagRepository;
    @Mock
    private AsyncJobManager asyncJobManager;
    @Mock
    private VectorSearchService vectorSearchService;
    @Mock
    private S3PresignedUrlService s3PresignedUrlService;

    @InjectMocks
    private KnowledgeCardService knowledgeCardService;

    private UUID userId;
    private UUID otherUserId;
    private User user;
    private User otherUser;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        otherUserId = UUID.randomUUID();
        user = buildUser(userId);
        otherUser = buildUser(otherUserId);
    }

    @Test
    void getCardDetail_returnsDetailResponse() {
        KnowledgeCard card = buildCard(user);
        LocalDateTime collectedAt = LocalDateTime.of(2026, 5, 15, 10, 30);
        ReflectionTestUtils.setField(card.getScrap(), "createdAt", collectedAt);
        CardTag springTag = new CardTag(card, Tag.builder().tagName("Spring").build());
        CardTag javaTag = new CardTag(card, Tag.builder().tagName("Java").build());

        when(knowledgeCardRepository.findByCardIdWithScrapAndCategory(card.getCardId()))
                .thenReturn(Optional.of(card));
        when(cardTagRepository.findAllByKnowledgeCardInWithTag(List.of(card)))
                .thenReturn(List.of(springTag, javaTag));

        KnowledgeCardDetailResponse response = knowledgeCardService.getCardDetail(userId, card.getCardId());

        assertThat(response.title()).isEqualTo("Knowledge card title");
        assertThat(response.categoryId()).isEqualTo(card.getCategory().getCategoryId());
        assertThat(response.categoryName()).isEqualTo("Backend");
        assertThat(response.sourceType()).isEqualTo(SourceType.TEXT);
        assertThat(response.sourceContent()).isEqualTo("raw content");
        assertThat(response.refinedContent()).isEqualTo("refined content");
        assertThat(response.summary()).isEqualTo("summary");
        assertThat(response.tags()).containsExactly("Spring", "Java");
        assertThat(response.collectedAt()).isEqualTo(collectedAt);
    }

    @Test
    void getCardDetail_returnsImageSourceContentForImageScrap() {
        String imageObjectKey = "scrap/images/%s/image.png".formatted(userId);
        String imageUrl = "https://bucket.s3.amazonaws.com/scrap/images/image.png?signature=test";
        KnowledgeCard card = buildCard(user, SourceType.IMAGE, null, null, imageObjectKey);

        when(knowledgeCardRepository.findByCardIdWithScrapAndCategory(card.getCardId()))
                .thenReturn(Optional.of(card));
        when(cardTagRepository.findAllByKnowledgeCardInWithTag(List.of(card)))
                .thenReturn(List.of());
        when(s3PresignedUrlService.createDownloadPresignedUrl(imageObjectKey)).thenReturn(imageUrl);

        KnowledgeCardDetailResponse response = knowledgeCardService.getCardDetail(userId, card.getCardId());

        assertThat(response.sourceType()).isEqualTo(SourceType.IMAGE);
        assertThat(response.sourceContent()).isEqualTo(imageUrl);
    }

    @Test
    void getCardDetail_returnsLinkSourceContentForLinkScrap() {
        String sourceUrl = "https://example.com/article";
        KnowledgeCard card = buildCard(user, SourceType.LINK, sourceUrl, null, null);

        when(knowledgeCardRepository.findByCardIdWithScrapAndCategory(card.getCardId()))
                .thenReturn(Optional.of(card));
        when(cardTagRepository.findAllByKnowledgeCardInWithTag(List.of(card)))
                .thenReturn(List.of());

        KnowledgeCardDetailResponse response = knowledgeCardService.getCardDetail(userId, card.getCardId());

        assertThat(response.sourceType()).isEqualTo(SourceType.LINK);
        assertThat(response.sourceContent()).isEqualTo(sourceUrl);
    }

    @Test
    void getCardDetail_throwsCardNotFoundWhenCardDoesNotExist() {
        UUID cardId = UUID.randomUUID();
        when(knowledgeCardRepository.findByCardIdWithScrapAndCategory(cardId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> knowledgeCardService.getCardDetail(userId, cardId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", KnowledgeErrorCode.CARD_NOT_FOUND);
        verifyNoInteractions(cardTagRepository);
    }

    @Test
    void getCardDetail_throwsCardAccessDeniedWhenUserIsNotOwner() {
        KnowledgeCard card = buildCard(otherUser);
        when(knowledgeCardRepository.findByCardIdWithScrapAndCategory(card.getCardId()))
                .thenReturn(Optional.of(card));

        assertThatThrownBy(() -> knowledgeCardService.getCardDetail(userId, card.getCardId()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", KnowledgeErrorCode.CARD_ACCESS_DENIED);
        verifyNoInteractions(cardTagRepository);
    }

    @Test
    void updateRefinedContent_updatesScrapRefinedContentAndReturnsDetail() {
        KnowledgeCard card = buildCard(user);
        CardTag springTag = new CardTag(card, Tag.builder().tagName("Spring").build());
        RefinedContentUpdateRequest request = new RefinedContentUpdateRequest(" updated refined content ");

        when(knowledgeCardRepository.findByCardIdWithScrapAndCategory(card.getCardId()))
                .thenReturn(Optional.of(card));
        when(cardTagRepository.findAllByKnowledgeCardInWithTag(List.of(card)))
                .thenReturn(List.of(springTag));

        KnowledgeCardDetailResponse response = knowledgeCardService.updateRefinedContent(
                userId,
                card.getCardId(),
                request
        );

        assertThat(card.getScrap().getRefinedContent()).isEqualTo("updated refined content");
        assertThat(response.refinedContent()).isEqualTo("updated refined content");
        assertThat(response.tags()).containsExactly("Spring");
    }

    @Test
    void updateRefinedContent_throwsCardNotFoundWhenCardDoesNotExist() {
        UUID cardId = UUID.randomUUID();
        RefinedContentUpdateRequest request = new RefinedContentUpdateRequest("updated refined content");
        when(knowledgeCardRepository.findByCardIdWithScrapAndCategory(cardId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> knowledgeCardService.updateRefinedContent(userId, cardId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", KnowledgeErrorCode.CARD_NOT_FOUND);
        verifyNoInteractions(cardTagRepository);
    }

    @Test
    void updateRefinedContent_throwsCardAccessDeniedWhenUserIsNotOwner() {
        KnowledgeCard card = buildCard(otherUser);
        RefinedContentUpdateRequest request = new RefinedContentUpdateRequest("updated refined content");
        when(knowledgeCardRepository.findByCardIdWithScrapAndCategory(card.getCardId()))
                .thenReturn(Optional.of(card));

        assertThatThrownBy(() -> knowledgeCardService.updateRefinedContent(userId, card.getCardId(), request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", KnowledgeErrorCode.CARD_ACCESS_DENIED);
        assertThat(card.getScrap().getRefinedContent()).isEqualTo("refined content");
        verifyNoInteractions(cardTagRepository);
    }

    @Test
    void getCardIdByScrap_throwsScrapAccessDeniedWhenUserIsNotOwner() {
        Scrap scrap = buildScrap(otherUser);
        when(scrapRepository.findById(scrap.getScrapId())).thenReturn(Optional.of(scrap));

        assertThatThrownBy(() -> knowledgeCardService.getCardIdByScrap(userId, scrap.getScrapId()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ScrapErrorCode.SCRAP_ACCESS_DENIED);
        verifyNoInteractions(cardTagRepository);
    }

    @Test
    void deleteCard_deletesCard() {
        KnowledgeCard card = buildCard(user);
        when(knowledgeCardRepository.findByCardIdWithScrapAndCategory(card.getCardId()))
                .thenReturn(Optional.of(card));

        knowledgeCardService.deleteCard(userId, card.getCardId());

        assertThat(card.isDeleted()).isTrue();
        verifyNoInteractions(cardTagRepository);
    }

    @Test
    void deleteCard_throwsCardNotFoundWhenCardDoesNotExist() {
        UUID cardId = UUID.randomUUID();
        when(knowledgeCardRepository.findByCardIdWithScrapAndCategory(cardId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> knowledgeCardService.deleteCard(userId, cardId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", KnowledgeErrorCode.CARD_NOT_FOUND);
        verifyNoInteractions(cardTagRepository);
    }

    @Test
    void deleteCard_throwsCardAccessDeniedWhenUserIsNotOwner() {
        KnowledgeCard card = buildCard(otherUser);
        when(knowledgeCardRepository.findByCardIdWithScrapAndCategory(card.getCardId()))
                .thenReturn(Optional.of(card));

        assertThatThrownBy(() -> knowledgeCardService.deleteCard(userId, card.getCardId()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", KnowledgeErrorCode.CARD_ACCESS_DENIED);
        assertThat(card.isDeleted()).isFalse();
        verifyNoInteractions(cardTagRepository);
    }

    private User buildUser(UUID id) {
        User user = User.builder()
                .username("user_" + id.toString().substring(0, 8))
                .provider(AuthProvider.LOCAL)
                .build();
        ReflectionTestUtils.setField(user, "userId", id);
        return user;
    }

    private KnowledgeCard buildCard(User owner) {
        return buildCard(owner, SourceType.TEXT, null, "raw content", null);
    }

    private KnowledgeCard buildCard(
            User owner,
            SourceType sourceType,
            String sourceUrl,
            String rawContent,
            String imageObjectKey
    ) {
        Scrap scrap = buildScrap(owner);
        ReflectionTestUtils.setField(scrap, "sourceType", sourceType);
        ReflectionTestUtils.setField(scrap, "sourceUrl", sourceUrl);
        ReflectionTestUtils.setField(scrap, "rawContent", rawContent);
        ReflectionTestUtils.setField(scrap, "imageObjectKey", imageObjectKey);
        Category category = Category.builder()
                .user(owner)
                .categoryName("Backend")
                .build();
        return KnowledgeCard.builder()
                .scrap(scrap)
                .category(category)
                .title("Knowledge card title")
                .summary("summary")
                .embedding(new float[]{0.1f, 0.2f})
                .build();
    }

    private Scrap buildScrap(User owner) {
        return Scrap.builder()
                .user(owner)
                .sourceType(SourceType.TEXT)
                .rawContent("raw content")
                .refinedContent("refined content")
                .build();
    }
}
