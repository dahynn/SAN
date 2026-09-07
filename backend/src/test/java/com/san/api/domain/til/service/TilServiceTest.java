package com.san.api.domain.til.service;

import com.san.api.domain.knowledge.entity.CardTag;
import com.san.api.domain.knowledge.entity.Category;
import com.san.api.domain.knowledge.entity.KnowledgeCard;
import com.san.api.domain.knowledge.repository.CardTagRepository;
import com.san.api.domain.knowledge.repository.KnowledgeCardRepository;
import com.san.api.domain.knowledge.service.VectorSearchService;
import com.san.api.domain.scrap.entity.Scrap;
import com.san.api.domain.scrap.entity.SourceType;
import com.san.api.domain.til.dto.request.TilGenerateRequest;
import com.san.api.domain.til.dto.request.TilUpdateRequest;
import com.san.api.domain.til.dto.response.TilGenerationJobResponse;
import com.san.api.domain.til.dto.response.TilRecallCardsResponse;
import com.san.api.domain.til.dto.response.TilResponse;
import com.san.api.domain.til.entity.DailySummary;
import com.san.api.domain.til.repository.DailySummaryRepository;
import com.san.api.domain.user.entity.AuthProvider;
import com.san.api.domain.user.entity.User;
import com.san.api.global.async.entity.JobStatus;
import com.san.api.global.async.entity.JobType;
import com.san.api.global.async.service.AsyncJobManager;
import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.CommonErrorCode;
import com.san.api.global.exception.errorcode.TilErrorCode;
import com.san.api.global.external.ai.client.AiEmbeddingClient;
import com.san.api.global.external.s3.service.S3PresignedUrlService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TilServiceTest {

    @Mock
    private DailySummaryRepository dailySummaryRepository;
    @Mock
    private DailySummaryService dailySummaryService;
    @Mock
    private AsyncJobManager asyncJobManager;
    @Mock
    private VectorSearchService vectorSearchService;
    @Mock
    private CardTagRepository cardTagRepository;
    @Mock
    private KnowledgeCardRepository knowledgeCardRepository;
    @Mock
    private S3PresignedUrlService s3PresignedUrlService;
    @Mock
    private AiEmbeddingClient aiEmbeddingClient;

    @InjectMocks
    private TilService tilService;

    private UUID userId;
    private UUID summaryId;
    private User user;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        summaryId = UUID.randomUUID();
        user = buildUser(userId);
    }

    @Test
    void requestGeneration_createsNewSummaryAndEnqueuesJobEveryTime() {
        LocalDate targetDate = LocalDate.of(2026, 5, 6);
        DailySummary summary = buildSummary(UUID.randomUUID(), user, targetDate, null, null);
        UUID jobId = UUID.randomUUID();

        when(dailySummaryService.createSummary(userId, targetDate)).thenReturn(summary);
        when(asyncJobManager.enqueueInCurrentTransaction(JobType.TIL_GENERATION, summary.getSummaryId())).thenReturn(jobId);

        TilGenerationJobResponse response = tilService.requestGeneration(userId, new TilGenerateRequest(targetDate));

        assertThat(response.summaryId()).isEqualTo(summary.getSummaryId());
        assertThat(response.jobId()).isEqualTo(jobId);
        assertThat(response.targetDate()).isEqualTo(targetDate);
        verify(dailySummaryRepository).acquireGenerationLock(userId);
        verify(dailySummaryService).createSummary(userId, targetDate);
        verify(asyncJobManager).enqueueInCurrentTransaction(JobType.TIL_GENERATION, summary.getSummaryId());
    }

    @Test
    void requestGeneration_duplicateActiveJob_throwsException() {
        LocalDate targetDate = LocalDate.of(2026, 5, 6);

        when(dailySummaryRepository.existsActiveJobForUser(
                userId,
                JobType.TIL_GENERATION,
                List.of(JobStatus.PENDING, JobStatus.PROCESSING)
        )).thenReturn(true);

        assertThatThrownBy(() -> tilService.requestGeneration(userId, new TilGenerateRequest(targetDate)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", CommonErrorCode.DUPLICATE_RESOURCE);
        verify(dailySummaryRepository).acquireGenerationLock(userId);
        verifyNoInteractions(dailySummaryService);
        verify(asyncJobManager, never()).enqueueInCurrentTransaction(any(), any());
    }

    @Test
    void getTil_returnsTilListForSameDate() {
        LocalDate targetDate = LocalDate.of(2026, 5, 6);
        DailySummary latestSummary = buildSummary(UUID.randomUUID(), user, targetDate, "두 번째 TIL", "content 2");
        DailySummary firstSummary = buildSummary(UUID.randomUUID(), user, targetDate, "첫 번째 TIL", "content 1");

        when(dailySummaryRepository.findAllByUser_UserIdAndTargetDateOrderByCreatedAtDesc(userId, targetDate))
                .thenReturn(List.of(latestSummary, firstSummary));

        List<TilResponse> response = tilService.getTil(userId, targetDate);

        assertThat(response).hasSize(2);
        assertThat(response).extracting(TilResponse::summaryId)
                .containsExactly(latestSummary.getSummaryId(), firstSummary.getSummaryId());
        assertThat(response).extracting(TilResponse::title)
                .containsExactly("두 번째 TIL", "첫 번째 TIL");
    }

    @Test
    void getTil_returnsEmptyListWhenNoTilExists() {
        LocalDate targetDate = LocalDate.of(2026, 5, 6);
        when(dailySummaryRepository.findAllByUser_UserIdAndTargetDateOrderByCreatedAtDesc(userId, targetDate))
                .thenReturn(List.of());

        List<TilResponse> response = tilService.getTil(userId, targetDate);

        assertThat(response).isEmpty();
    }

    @Test
    void updateTil_updatesTitleContentAndEmbedding() {
        DailySummary summary = buildSummary(summaryId, user, LocalDate.of(2026, 5, 6), "old title", "old content");
        TilUpdateRequest request = new TilUpdateRequest("new title", "new content");
        float[] embedding = new float[]{0.3f, 0.4f};

        when(dailySummaryRepository.findBySummaryIdWithUser(summaryId)).thenReturn(Optional.of(summary));
        when(aiEmbeddingClient.embed(request.content())).thenReturn(embedding);

        TilResponse response = tilService.updateTil(summaryId, userId, request);

        assertThat(response.summaryId()).isEqualTo(summaryId);
        assertThat(response.title()).isEqualTo("new title");
        assertThat(response.content()).isEqualTo("new content");
        assertThat(summary.getEmbedding()).isEqualTo(embedding);
        verify(aiEmbeddingClient).embed("new content");
    }

    @Test
    void updateTil_notFound_throwsException() {
        TilUpdateRequest request = new TilUpdateRequest("new title", "new content");

        when(dailySummaryRepository.findBySummaryIdWithUser(summaryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tilService.updateTil(summaryId, userId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", TilErrorCode.SUMMARY_NOT_FOUND);
        verifyNoInteractions(aiEmbeddingClient);
    }

    @Test
    void updateTil_otherUser_throwsException() {
        UUID otherUserId = UUID.randomUUID();
        DailySummary summary = buildSummary(summaryId, user, LocalDate.of(2026, 5, 6), "old title", "old content");
        TilUpdateRequest request = new TilUpdateRequest("new title", "new content");

        when(dailySummaryRepository.findBySummaryIdWithUser(summaryId)).thenReturn(Optional.of(summary));

        assertThatThrownBy(() -> tilService.updateTil(summaryId, otherUserId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", TilErrorCode.SUMMARY_ACCESS_DENIED);
        verifyNoInteractions(aiEmbeddingClient);
    }

    @Test
    void deleteTil_deletesSummary() {
        DailySummary summary = buildSummary(summaryId, user, LocalDate.of(2026, 5, 6), "TIL", "content");

        when(dailySummaryRepository.findBySummaryIdWithUser(summaryId)).thenReturn(Optional.of(summary));

        tilService.deleteTil(summaryId, userId);

        assertThat(summary.isDeleted()).isTrue();
    }

    @Test
    void deleteTil_notFound_throwsException() {
        when(dailySummaryRepository.findBySummaryIdWithUser(summaryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tilService.deleteTil(summaryId, userId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", TilErrorCode.SUMMARY_NOT_FOUND);
    }

    @Test
    void deleteTil_otherUser_throwsException() {
        UUID otherUserId = UUID.randomUUID();
        DailySummary summary = buildSummary(summaryId, user, LocalDate.of(2026, 5, 6), "TIL", "content");

        when(dailySummaryRepository.findBySummaryIdWithUser(summaryId)).thenReturn(Optional.of(summary));

        assertThatThrownBy(() -> tilService.deleteTil(summaryId, otherUserId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", TilErrorCode.SUMMARY_ACCESS_DENIED);
        assertThat(summary.isDeleted()).isFalse();
    }

    @Test
    void getSources_convertsImageObjectKeyToImageUrl() {
        LocalDate targetDate = LocalDate.of(2026, 5, 6);
        DailySummary summary = buildSummary(summaryId, user, targetDate, "TIL", "content");
        String imageObjectKey = "scrap/images/%s/image.png".formatted(userId);
        String imageUrl = "https://bucket.s3.us-east-1.amazonaws.com/scrap/images/image.png?signature=test";
        KnowledgeCard card = buildImageCard(UUID.randomUUID(), user, imageObjectKey);

        when(dailySummaryRepository.findBySummaryIdWithUser(summaryId)).thenReturn(Optional.of(summary));
        when(knowledgeCardRepository.findTilSourceCards(eq(userId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(card));
        when(s3PresignedUrlService.createDownloadPresignedUrl(imageObjectKey)).thenReturn(imageUrl);

        var response = tilService.getSources(summaryId, userId);

        assertThat(response.sources()).hasSize(1);
        assertThat(response.sources().get(0).imageUrl()).isEqualTo(imageUrl);
    }

    @Test
    void getSources_returnsNullImageUrlWhenImageObjectKeyIsBlank() {
        LocalDate targetDate = LocalDate.of(2026, 5, 6);
        DailySummary summary = buildSummary(summaryId, user, targetDate, "TIL", "content");
        KnowledgeCard card = buildImageCard(UUID.randomUUID(), user, "   ");

        when(dailySummaryRepository.findBySummaryIdWithUser(summaryId)).thenReturn(Optional.of(summary));
        when(knowledgeCardRepository.findTilSourceCards(eq(userId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(card));

        var response = tilService.getSources(summaryId, userId);

        assertThat(response.sources()).hasSize(1);
        assertThat(response.sources().get(0).imageUrl()).isNull();
        verifyNoInteractions(s3PresignedUrlService);
    }

    @Test
    void getRecallCards_유사카드없음_태그조회없이_빈응답반환() {
        when(vectorSearchService.findRelatedByTil(summaryId, userId)).thenReturn(List.of());

        TilRecallCardsResponse response = tilService.getRecallCards(summaryId, userId);

        assertThat(response.recallCards()).isEmpty();
        verifyNoInteractions(cardTagRepository);
    }

    @Test
    void getRecallCards_유사카드있음_태그포함_응답반환() {
        KnowledgeCard card = buildCard(UUID.randomUUID(), user);
        CardTag cardTag = buildCardTag(card, "Spring");

        when(vectorSearchService.findRelatedByTil(summaryId, userId)).thenReturn(List.of(card));
        when(cardTagRepository.findAllByKnowledgeCardInWithTag(eq(List.of(card)))).thenReturn(List.of(cardTag));

        TilRecallCardsResponse response = tilService.getRecallCards(summaryId, userId);

        assertThat(response.recallCards()).hasSize(1);
        assertThat(response.recallCards().get(0).title()).isEqualTo("테스트 카드");
        assertThat(response.recallCards().get(0).tags()).hasSize(1);
        assertThat(response.recallCards().get(0).tags().get(0).tagName()).isEqualTo("Spring");
        verify(cardTagRepository).findAllByKnowledgeCardInWithTag(List.of(card));
    }

    @Test
    void getRecallCards_태그없는카드_빈태그리스트로_응답반환() {
        KnowledgeCard card = buildCard(UUID.randomUUID(), user);

        when(vectorSearchService.findRelatedByTil(summaryId, userId)).thenReturn(List.of(card));
        when(cardTagRepository.findAllByKnowledgeCardInWithTag(eq(List.of(card)))).thenReturn(List.of());

        TilRecallCardsResponse response = tilService.getRecallCards(summaryId, userId);

        assertThat(response.recallCards()).hasSize(1);
        assertThat(response.recallCards().get(0).tags()).isEmpty();
    }

    // ───────────────────────────────────────────────
    // 헬퍼 메서드
    // ───────────────────────────────────────────────

    private User buildUser(UUID id) {
        User u = User.builder()
                .username("user_" + id.toString().substring(0, 8))
                .provider(AuthProvider.LOCAL)
                .build();
        ReflectionTestUtils.setField(u, "userId", id);
        return u;
    }

    private DailySummary buildSummary(UUID id, User owner, LocalDate targetDate, String title, String content) {
        DailySummary summary = DailySummary.builder()
                .user(owner)
                .targetDate(targetDate)
                .title(title)
                .content(content)
                .build();
        ReflectionTestUtils.setField(summary, "summaryId", id);
        return summary;
    }

    private KnowledgeCard buildCard(UUID cardId, User owner) {
        Scrap scrap = Scrap.builder()
                .user(owner)
                .sourceType(SourceType.TEXT)
                .rawContent("content")
                .build();
        Category category = Category.builder()
                .user(owner)
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

    private KnowledgeCard buildImageCard(UUID cardId, User owner, String imageObjectKey) {
        Scrap scrap = Scrap.builder()
                .user(owner)
                .sourceType(SourceType.IMAGE)
                .rawContent("image")
                .imageObjectKey(imageObjectKey)
                .build();
        Category category = Category.builder()
                .user(owner)
                .categoryName("image")
                .build();
        KnowledgeCard card = KnowledgeCard.builder()
                .scrap(scrap)
                .category(category)
                .title("image card")
                .summary("summary")
                .embedding(new float[]{0.1f, 0.2f})
                .build();
        ReflectionTestUtils.setField(card, "cardId", cardId);
        return card;
    }

    private CardTag buildCardTag(KnowledgeCard card, String tagName) {
        com.san.api.domain.knowledge.entity.Tag tag = com.san.api.domain.knowledge.entity.Tag.builder()
                .tagName(tagName)
                .build();
        return new CardTag(card, tag);
    }
}
