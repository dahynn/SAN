package com.san.api.domain.knowledge.service;

import com.san.api.domain.knowledge.dto.response.SearchResponse;
import com.san.api.domain.knowledge.entity.Category;
import com.san.api.domain.knowledge.entity.KnowledgeCard;
import com.san.api.domain.knowledge.repository.KnowledgeCardRepository;
import com.san.api.domain.scrap.entity.Scrap;
import com.san.api.domain.scrap.entity.SourceType;
import com.san.api.domain.user.entity.AuthProvider;
import com.san.api.domain.user.entity.User;
import com.san.api.global.external.ai.client.AiEmbeddingClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/** HybridSearchService 병합 우선순위·페이지네이션 단위 테스트. */
@ExtendWith(MockitoExtension.class)
class HybridSearchServiceTest {

    @Mock
    private KnowledgeCardRepository knowledgeCardRepository;
    @Mock
    private AiEmbeddingClient aiEmbeddingClient;

    @InjectMocks
    private HybridSearchService hybridSearchService;

    private UUID userId;
    private User user;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = buildUser(userId);
        when(aiEmbeddingClient.embed(anyString())).thenReturn(new float[]{0.1f, 0.2f});
    }

    // ───────────────────────────────────────────────
    // 병합 우선순위
    // ───────────────────────────────────────────────

    @Test
    void 두검색_모두_히트한_카드가_최상위() {
        KnowledgeCard both = buildCard(UUID.randomUUID());
        KnowledgeCard vectorOnly = buildCard(UUID.randomUUID());
        KnowledgeCard keywordOnly = buildCard(UUID.randomUUID());

        stubVector(List.of(both, vectorOnly));
        stubKeyword(List.of(both, keywordOnly));

        SearchResponse response = search(0, 10);

        assertThat(response.results()).extracting("cardId")
                .containsExactly(both.getCardId(), vectorOnly.getCardId(), keywordOnly.getCardId());
    }

    @Test
    void 벡터만_히트한_카드가_키워드전용보다_앞() {
        KnowledgeCard vectorOnly = buildCard(UUID.randomUUID());
        KnowledgeCard keywordOnly = buildCard(UUID.randomUUID());

        stubVector(List.of(vectorOnly));
        stubKeyword(List.of(keywordOnly));

        SearchResponse response = search(0, 10);

        assertThat(response.results()).extracting("cardId")
                .containsExactly(vectorOnly.getCardId(), keywordOnly.getCardId());
    }

    @Test
    void 벡터_결과없으면_키워드결과만_반환() {
        KnowledgeCard keywordOnly = buildCard(UUID.randomUUID());

        stubVector(List.of());
        stubKeyword(List.of(keywordOnly));

        SearchResponse response = search(0, 10);

        assertThat(response.results()).extracting("cardId")
                .containsExactly(keywordOnly.getCardId());
    }

    @Test
    void 키워드_결과없으면_벡터결과만_반환() {
        KnowledgeCard vectorOnly = buildCard(UUID.randomUUID());

        stubVector(List.of(vectorOnly));
        stubKeyword(List.of());

        SearchResponse response = search(0, 10);

        assertThat(response.results()).extracting("cardId")
                .containsExactly(vectorOnly.getCardId());
    }

    @Test
    void 두검색_모두_결과없으면_빈리스트반환() {
        stubVector(List.of());
        stubKeyword(List.of());

        SearchResponse response = search(0, 10);

        assertThat(response.results()).isEmpty();
        assertThat(response.totalCount()).isZero();
        assertThat(response.hasNext()).isFalse();
    }

    @Test
    void 중복카드는_한번만_포함() {
        KnowledgeCard card = buildCard(UUID.randomUUID());

        stubVector(List.of(card));
        stubKeyword(List.of(card));

        SearchResponse response = search(0, 10);

        assertThat(response.results()).hasSize(1);
        assertThat(response.totalCount()).isEqualTo(1L);
    }

    // ───────────────────────────────────────────────
    // 페이지네이션
    // ───────────────────────────────────────────────

    @Test
    void 페이지네이션_첫페이지() {
        List<KnowledgeCard> cards = buildCards(5);
        stubVector(cards);
        stubKeyword(List.of());

        SearchResponse response = search(0, 3);

        assertThat(response.results()).hasSize(3);
        assertThat(response.totalCount()).isEqualTo(5L);
        assertThat(response.hasNext()).isTrue();
    }

    @Test
    void 페이지네이션_마지막페이지() {
        List<KnowledgeCard> cards = buildCards(5);
        stubVector(cards);
        stubKeyword(List.of());

        SearchResponse response = search(1, 3);

        assertThat(response.results()).hasSize(2);
        assertThat(response.hasNext()).isFalse();
    }

    @Test
    void totalCount는_병합후_중복제거된_전체수() {
        KnowledgeCard both = buildCard(UUID.randomUUID());
        KnowledgeCard vectorOnly = buildCard(UUID.randomUUID());
        KnowledgeCard keywordOnly = buildCard(UUID.randomUUID());

        stubVector(List.of(both, vectorOnly));
        stubKeyword(List.of(both, keywordOnly));

        SearchResponse response = search(0, 10);

        assertThat(response.totalCount()).isEqualTo(3L);
    }

    // ───────────────────────────────────────────────
    // 헬퍼 메서드
    // ───────────────────────────────────────────────

    private SearchResponse search(int page, int size) {
        return hybridSearchService.search("AOP", userId, null, null, null, null, page, size);
    }

    private void stubVector(List<KnowledgeCard> result) {
        when(knowledgeCardRepository.searchByVectorWithFilters(
                anyString(), eq(userId), isNull(), isNull(), isNull(), isNull(), eq(0.3d), anyInt(), eq(0)))
                .thenReturn(result);
    }

    private void stubKeyword(List<KnowledgeCard> result) {
        when(knowledgeCardRepository.searchByKeyword(
                anyString(), eq(userId), isNull(), isNull(), isNull(), isNull(), anyInt()))
                .thenReturn(result);
    }

    private User buildUser(UUID id) {
        User u = User.builder()
                .username("user_" + id.toString().substring(0, 8))
                .provider(AuthProvider.LOCAL)
                .build();
        ReflectionTestUtils.setField(u, "userId", id);
        return u;
    }

    private KnowledgeCard buildCard(UUID cardId) {
        Scrap scrap = Scrap.builder()
                .user(user)
                .sourceType(SourceType.TEXT)
                .rawContent("content")
                .build();
        Category category = Category.builder()
                .user(user)
                .categoryName("테스트")
                .build();
        KnowledgeCard card = KnowledgeCard.builder()
                .scrap(scrap)
                .category(category)
                .title("테스트 카드")
                .summary("요약")
                .embedding(new float[]{0.1f, 0.2f})
                .build();
        ReflectionTestUtils.setField(card, "cardId", cardId);
        return card;
    }

    private List<KnowledgeCard> buildCards(int count) {
        return java.util.stream.IntStream.range(0, count)
                .mapToObj(i -> buildCard(UUID.randomUUID()))
                .toList();
    }
}
