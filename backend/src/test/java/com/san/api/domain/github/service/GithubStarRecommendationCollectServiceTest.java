package com.san.api.domain.github.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.san.api.domain.github.dto.response.GithubStarRecommendationCollectResponse;
import com.san.api.domain.github.entity.GithubStarRecommendation;
import com.san.api.domain.github.repository.GithubStarRecommendationRepository;
import com.san.api.domain.knowledge.entity.Category;
import com.san.api.domain.knowledge.entity.KnowledgeCard;
import com.san.api.domain.knowledge.entity.Tag;
import com.san.api.domain.knowledge.repository.CardTagRepository;
import com.san.api.domain.knowledge.repository.CategoryRepository;
import com.san.api.domain.knowledge.repository.KnowledgeCardRepository;
import com.san.api.domain.knowledge.repository.TagRepository;
import com.san.api.domain.scrap.entity.Scrap;
import com.san.api.domain.scrap.entity.SourceType;
import com.san.api.domain.scrap.repository.ScrapRepository;
import com.san.api.domain.scrap.service.ScrapContentHashPolicy;
import com.san.api.domain.user.entity.AuthProvider;
import com.san.api.domain.user.entity.User;
import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.CommonErrorCode;
import com.san.api.global.external.ai.dto.response.AiAnalyzeResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GithubStarRecommendationCollectServiceTest {

    private GithubStarRecommendationRepository githubStarRecommendationRepository;
    private ScrapRepository scrapRepository;
    private KnowledgeCardRepository knowledgeCardRepository;
    private CategoryRepository categoryRepository;
    private TagRepository tagRepository;
    private CardTagRepository cardTagRepository;
    private GithubStarRecommendationCollectService service;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        githubStarRecommendationRepository = mock(GithubStarRecommendationRepository.class);
        scrapRepository = mock(ScrapRepository.class);
        knowledgeCardRepository = mock(KnowledgeCardRepository.class);
        categoryRepository = mock(CategoryRepository.class);
        tagRepository = mock(TagRepository.class);
        cardTagRepository = mock(CardTagRepository.class);
        objectMapper = new ObjectMapper();
        service = new GithubStarRecommendationCollectService(
                githubStarRecommendationRepository,
                scrapRepository,
                knowledgeCardRepository,
                categoryRepository,
                tagRepository,
                cardTagRepository,
                new ScrapContentHashPolicy(),
                objectMapper
        );
    }

    @Test
    void collect_createsScrapAndKnowledgeCardFromSavedAnalysisResult() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID recommendationId = UUID.randomUUID();
        User user = buildUser(userId);
        GithubStarRecommendation recommendation = buildRecommendation(user, recommendationId);
        AiAnalyzeResponse analysis = new AiAnalyzeResponse(
                "Analyzed title",
                "Analyzed summary",
                List.of("Spring"),
                "Backend",
                new float[]{0.1f}
        );
        ReflectionTestUtils.setField(recommendation, "analysisResult", objectMapper.writeValueAsString(analysis));
        Category category = Category.builder()
                .user(user)
                .categoryName("Backend")
                .build();

        when(githubStarRecommendationRepository.findByIdAndUserIdForUpdate(recommendationId, userId))
                .thenReturn(Optional.of(recommendation));
        when(scrapRepository.findByUser_UserIdAndSourceTypeAndContentHash(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(scrapRepository.saveAndFlush(any(Scrap.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(knowledgeCardRepository.findByScrapIdWithCategory(any())).thenReturn(Optional.empty());
        when(categoryRepository.findByUser_UserIdAndCategoryName(userId, "Backend")).thenReturn(Optional.of(category));
        when(knowledgeCardRepository.saveAndFlush(any(KnowledgeCard.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(tagRepository.findByTagName("Spring")).thenReturn(Optional.empty());
        when(tagRepository.saveAndFlush(any(Tag.class))).thenAnswer(invocation -> invocation.getArgument(0));

        GithubStarRecommendationCollectResponse response = service.collect(userId, recommendationId);

        assertThat(response.recommendationId()).isEqualTo(recommendationId);
        assertThat(response.scrapId()).isNotNull();
        assertThat(response.cardId()).isNotNull();
        assertThat(response.collected()).isTrue();
        assertThat(recommendation.isCollected()).isTrue();
        assertThat(recommendation.getCollectedTargetId()).isEqualTo(response.cardId());
        verify(cardTagRepository).save(any());
    }

    @Test
    void collect_throwsWhenRecommendationAlreadyCollected() {
        UUID userId = UUID.randomUUID();
        UUID recommendationId = UUID.randomUUID();
        GithubStarRecommendation recommendation = buildRecommendation(buildUser(userId), recommendationId);
        recommendation.markCollected(UUID.randomUUID());

        when(githubStarRecommendationRepository.findByIdAndUserIdForUpdate(recommendationId, userId))
                .thenReturn(Optional.of(recommendation));

        assertThatThrownBy(() -> service.collect(userId, recommendationId))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(CommonErrorCode.DUPLICATE_RESOURCE));
    }

    @Test
    void collect_throwsWhenRecommendationDoesNotExistForUser() {
        UUID userId = UUID.randomUUID();
        UUID recommendationId = UUID.randomUUID();
        when(githubStarRecommendationRepository.findByIdAndUserIdForUpdate(recommendationId, userId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.collect(userId, recommendationId))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(CommonErrorCode.RESOURCE_NOT_FOUND));
    }

    @Test
    void collect_reusesExistingScrapWhenSaveConflicts() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID recommendationId = UUID.randomUUID();
        User user = buildUser(userId);
        GithubStarRecommendation recommendation = buildRecommendation(user, recommendationId);
        AiAnalyzeResponse analysis = new AiAnalyzeResponse(
                "Analyzed title",
                "Analyzed summary",
                List.of(),
                "Backend",
                new float[]{0.1f}
        );
        ReflectionTestUtils.setField(recommendation, "analysisResult", objectMapper.writeValueAsString(analysis));
        Scrap existingScrap = Scrap.builder()
                .user(user)
                .sourceType(SourceType.LINK)
                .sourceUrl("https://example.com")
                .rawContent("https://example.com")
                .contentHash("hash")
                .build();
        Category category = Category.builder()
                .user(user)
                .categoryName("Backend")
                .build();

        when(githubStarRecommendationRepository.findByIdAndUserIdForUpdate(recommendationId, userId))
                .thenReturn(Optional.of(recommendation));
        when(scrapRepository.findByUser_UserIdAndSourceTypeAndContentHash(any(), any(), any()))
                .thenReturn(Optional.empty(), Optional.of(existingScrap));
        when(scrapRepository.saveAndFlush(any(Scrap.class))).thenThrow(new DataIntegrityViolationException("duplicate"));
        when(knowledgeCardRepository.findByScrapIdWithCategory(existingScrap.getScrapId())).thenReturn(Optional.empty());
        when(categoryRepository.findByUser_UserIdAndCategoryName(userId, "Backend")).thenReturn(Optional.of(category));
        when(knowledgeCardRepository.saveAndFlush(any(KnowledgeCard.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        GithubStarRecommendationCollectResponse response = service.collect(userId, recommendationId);

        assertThat(response.scrapId()).isEqualTo(existingScrap.getScrapId());
        assertThat(response.collected()).isTrue();
    }

    @Test
    void collect_reusesExistingCardWhenSaveConflicts() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID recommendationId = UUID.randomUUID();
        User user = buildUser(userId);
        GithubStarRecommendation recommendation = buildRecommendation(user, recommendationId);
        AiAnalyzeResponse analysis = new AiAnalyzeResponse(
                "Analyzed title",
                "Analyzed summary",
                List.of(),
                "Backend",
                new float[]{0.1f}
        );
        ReflectionTestUtils.setField(recommendation, "analysisResult", objectMapper.writeValueAsString(analysis));
        Scrap scrap = Scrap.builder()
                .user(user)
                .sourceType(SourceType.LINK)
                .sourceUrl("https://example.com")
                .rawContent("https://example.com")
                .contentHash("hash")
                .build();
        Category category = Category.builder()
                .user(user)
                .categoryName("Backend")
                .build();
        KnowledgeCard existingCard = KnowledgeCard.builder()
                .scrap(scrap)
                .category(category)
                .title("Existing title")
                .summary("Existing summary")
                .embedding(new float[]{0.1f})
                .build();

        when(githubStarRecommendationRepository.findByIdAndUserIdForUpdate(recommendationId, userId))
                .thenReturn(Optional.of(recommendation));
        when(scrapRepository.findByUser_UserIdAndSourceTypeAndContentHash(any(), any(), any()))
                .thenReturn(Optional.of(scrap));
        when(knowledgeCardRepository.findByScrapIdWithCategory(scrap.getScrapId()))
                .thenReturn(Optional.empty(), Optional.of(existingCard));
        when(categoryRepository.findByUser_UserIdAndCategoryName(userId, "Backend")).thenReturn(Optional.of(category));
        when(knowledgeCardRepository.saveAndFlush(any(KnowledgeCard.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        GithubStarRecommendationCollectResponse response = service.collect(userId, recommendationId);

        assertThat(response.cardId()).isEqualTo(existingCard.getCardId());
        assertThat(response.collected()).isTrue();
    }

    @Test
    void collect_reusesExistingTagWhenSaveConflicts() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID recommendationId = UUID.randomUUID();
        User user = buildUser(userId);
        GithubStarRecommendation recommendation = buildRecommendation(user, recommendationId);
        AiAnalyzeResponse analysis = new AiAnalyzeResponse(
                "Analyzed title",
                "Analyzed summary",
                List.of("Spring"),
                "Backend",
                new float[]{0.1f}
        );
        ReflectionTestUtils.setField(recommendation, "analysisResult", objectMapper.writeValueAsString(analysis));
        Scrap scrap = Scrap.builder()
                .user(user)
                .sourceType(SourceType.LINK)
                .sourceUrl("https://example.com")
                .rawContent("https://example.com")
                .contentHash("hash")
                .build();
        Category category = Category.builder()
                .user(user)
                .categoryName("Backend")
                .build();
        Tag existingTag = Tag.builder()
                .tagName("Spring")
                .build();

        when(githubStarRecommendationRepository.findByIdAndUserIdForUpdate(recommendationId, userId))
                .thenReturn(Optional.of(recommendation));
        when(scrapRepository.findByUser_UserIdAndSourceTypeAndContentHash(any(), any(), any()))
                .thenReturn(Optional.of(scrap));
        when(knowledgeCardRepository.findByScrapIdWithCategory(scrap.getScrapId())).thenReturn(Optional.empty());
        when(categoryRepository.findByUser_UserIdAndCategoryName(userId, "Backend")).thenReturn(Optional.of(category));
        when(knowledgeCardRepository.saveAndFlush(any(KnowledgeCard.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(tagRepository.findByTagName("Spring"))
                .thenReturn(Optional.empty(), Optional.of(existingTag));
        when(tagRepository.saveAndFlush(any(Tag.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        GithubStarRecommendationCollectResponse response = service.collect(userId, recommendationId);

        assertThat(response.collected()).isTrue();
        verify(cardTagRepository).save(any());
    }

    private User buildUser(UUID userId) {
        User user = User.builder()
                .username("user@example.com")
                .provider(AuthProvider.LOCAL)
                .build();
        ReflectionTestUtils.setField(user, "userId", userId);
        return user;
    }

    private GithubStarRecommendation buildRecommendation(User user, UUID recommendationId) {
        GithubStarRecommendation recommendation = new GithubStarRecommendation(
                user,
                UUID.randomUUID(),
                "https://example.com",
                "title",
                "summary",
                "{\"title\":\"title\",\"summary\":\"summary\",\"tags\":[],\"category\":\"Backend\",\"embedding\":[0.1]}"
        );
        ReflectionTestUtils.setField(recommendation, "githubStarRecommendationId", recommendationId);
        return recommendation;
    }
}
