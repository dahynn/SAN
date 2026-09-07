package com.san.api.domain.recall.service;

import com.san.api.domain.knowledge.entity.Category;
import com.san.api.domain.knowledge.entity.KnowledgeCard;
import com.san.api.domain.knowledge.repository.KnowledgeCardRepository;
import com.san.api.domain.scrap.entity.Scrap;
import com.san.api.domain.scrap.entity.SourceType;
import com.san.api.domain.til.entity.DailySummary;
import com.san.api.domain.til.repository.DailySummaryRepository;
import com.san.api.domain.user.entity.AuthProvider;
import com.san.api.domain.user.entity.User;
import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.RecallErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecallQuizSourceServiceTest {

    @Mock
    private DailySummaryRepository dailySummaryRepository;
    @Mock
    private KnowledgeCardRepository knowledgeCardRepository;

    @InjectMocks
    private RecallQuizSourceService recallQuizSourceService;

    private UUID userId;
    private User user;
    private LocalDate targetDate;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = buildUser(userId);
        targetDate = LocalDate.of(2026, 5, 19);
    }

    @Test
    void findSourcesReturnsLatestSummaryAndSourceCards() {
        DailySummary summary = buildSummary();
        KnowledgeCard card = buildCard();

        when(dailySummaryRepository.findAllByUserIdAndTargetDateWithUserOrderByCreatedAtDesc(userId, targetDate))
                .thenReturn(List.of(summary));
        when(knowledgeCardRepository.findTilSourceCards(
                userId,
                targetDate.atStartOfDay(),
                targetDate.plusDays(1).atStartOfDay()
        )).thenReturn(List.of(card));

        RecallQuizSourceService.RecallQuizSourceResult result = recallQuizSourceService.findSources(userId, targetDate);

        assertThat(result.dailySummary()).isEqualTo(summary);
        assertThat(result.sourceCards()).containsExactly(card);
    }

    @Test
    void findSourcesThrowsExceptionWhenSummaryDoesNotExist() {
        when(dailySummaryRepository.findAllByUserIdAndTargetDateWithUserOrderByCreatedAtDesc(userId, targetDate))
                .thenReturn(List.of());

        assertThatThrownBy(() -> recallQuizSourceService.findSources(userId, targetDate))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", RecallErrorCode.RECALL_TIL_NOT_FOUND);

        verifyNoInteractions(knowledgeCardRepository);
    }

    @Test
    void findSourcesThrowsExceptionWhenSourceCardsAreEmpty() {
        DailySummary summary = buildSummary();

        when(dailySummaryRepository.findAllByUserIdAndTargetDateWithUserOrderByCreatedAtDesc(userId, targetDate))
                .thenReturn(List.of(summary));
        when(knowledgeCardRepository.findTilSourceCards(
                userId,
                targetDate.atStartOfDay(),
                targetDate.plusDays(1).atStartOfDay()
        )).thenReturn(List.of());

        assertThatThrownBy(() -> recallQuizSourceService.findSources(userId, targetDate))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", RecallErrorCode.EMPTY_RECALL_SOURCE);

        verify(knowledgeCardRepository).findTilSourceCards(
                userId,
                targetDate.atStartOfDay(),
                targetDate.plusDays(1).atStartOfDay()
        );
    }

    private DailySummary buildSummary() {
        return DailySummary.builder()
                .user(user)
                .targetDate(targetDate)
                .title("TIL title")
                .content("TIL content")
                .build();
    }

    private KnowledgeCard buildCard() {
        Scrap scrap = Scrap.builder()
                .user(user)
                .sourceType(SourceType.TEXT)
                .rawContent("raw content")
                .build();

        return KnowledgeCard.builder()
                .scrap(scrap)
                .category(Category.builder().user(user).categoryName("Backend").build())
                .title("Card title")
                .summary("Card summary")
                .build();
    }

    private User buildUser(UUID userId) {
        User user = User.builder()
                .username("testuser")
                .provider(AuthProvider.LOCAL)
                .build();
        ReflectionTestUtils.setField(user, "userId", userId);
        return user;
    }
}
