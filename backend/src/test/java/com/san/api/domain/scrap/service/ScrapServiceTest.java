package com.san.api.domain.scrap.service;

import com.san.api.domain.knowledge.entity.Category;
import com.san.api.domain.knowledge.entity.KnowledgeCard;
import com.san.api.domain.knowledge.repository.KnowledgeCardRepository;
import com.san.api.domain.scrap.dto.request.ScrapCreateRequest;
import com.san.api.domain.scrap.dto.response.ScrapResponse;
import com.san.api.domain.scrap.entity.Scrap;
import com.san.api.domain.scrap.entity.ScrapCardCreationStatus;
import com.san.api.domain.scrap.entity.ScrapOriginStatus;
import com.san.api.domain.scrap.entity.ScrapRefineStatus;
import com.san.api.domain.scrap.entity.SourceType;
import com.san.api.domain.scrap.repository.ScrapRepository;
import com.san.api.domain.user.entity.AuthProvider;
import com.san.api.domain.user.entity.User;
import com.san.api.domain.user.repository.UserRepository;
import com.san.api.global.async.entity.AsyncJob;
import com.san.api.global.async.entity.JobStatus;
import com.san.api.global.async.entity.JobType;
import com.san.api.global.async.repository.AsyncJobRepository;
import com.san.api.global.async.service.AsyncJobManager;
import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.CommonErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** ScrapService 비즈니스 로직 단위 테스트 */
@ExtendWith(MockitoExtension.class)
class ScrapServiceTest {

    @Mock
    private ScrapRepository scrapRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AsyncJobManager asyncJobManager;

    @Mock
    private AsyncJobRepository asyncJobRepository;

    @Mock
    private KnowledgeCardRepository knowledgeCardRepository;

    private ScrapContentHashPolicy contentHashPolicy;
    private ScrapService scrapService;

    @BeforeEach
    void setUp() {
        contentHashPolicy = new ScrapContentHashPolicy();
        scrapService = new ScrapService(
                scrapRepository,
                userRepository,
                new SourceTypeDetector(),
                contentHashPolicy,
                asyncJobManager,
                asyncJobRepository,
                knowledgeCardRepository
        );
    }

    @Test
    void createScrap_savesNormalizedRawContentAndContentHash() {
        UUID userId = UUID.randomUUID();
        User user = buildUser(userId);
        ScrapCreateRequest request = new ScrapCreateRequest(null, " hello\r\nworld \n", null);
        String contentHash = contentHashPolicy.createContentHash("hello\nworld");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(scrapRepository.findByUser_UserIdAndSourceTypeAndContentHash(userId, SourceType.TEXT, contentHash))
                .thenReturn(Optional.empty());
        when(scrapRepository.save(any(Scrap.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        UUID jobId = UUID.randomUUID();
        UUID refineJobId = UUID.randomUUID();
        when(knowledgeCardRepository.findByScrapIdWithCategory(any(UUID.class))).thenReturn(Optional.empty());
        when(asyncJobRepository.findByTargetIdAndJobType(any(UUID.class), eq(JobType.CARD_ANALYSIS)))
                .thenReturn(List.of());
        when(asyncJobManager.enqueue(eq(JobType.SCRAP_REFINE), any(UUID.class))).thenReturn(refineJobId);
        when(asyncJobManager.enqueue(eq(JobType.CARD_ANALYSIS), any(UUID.class))).thenReturn(jobId);

        ScrapResponse response = scrapService.createScrap(userId, request);

        ArgumentCaptor<Scrap> captor = ArgumentCaptor.forClass(Scrap.class);
        verify(scrapRepository).save(captor.capture());
        Scrap saved = captor.getValue();

        assertThat(saved.getSourceType()).isEqualTo(SourceType.TEXT);
        assertThat(saved.getRawContent()).isEqualTo("hello\nworld");
        assertThat(saved.getContentHash()).isEqualTo(contentHash);
        assertThat(response.analysisJobId()).isEqualTo(jobId);
        assertThat(response.refineJobId()).isEqualTo(refineJobId);
        assertThat(response.originStatus()).isEqualTo(ScrapOriginStatus.CREATED);
        assertThat(response.refineStatus()).isEqualTo(ScrapRefineStatus.REFINE_IN_PROGRESS);
        assertThat(response.cardCreationStatus()).isEqualTo(ScrapCardCreationStatus.ANALYSIS_IN_PROGRESS);
        verify(asyncJobManager).enqueue(JobType.SCRAP_REFINE, saved.getScrapId());
    }

    @Test
    void createScrap_savesImageScrapWhenImageObjectKeyExists() {
        UUID userId = UUID.randomUUID();
        User user = buildUser(userId);
        String imageObjectKey = "scrap/images/%s/image.png".formatted(userId);
        ScrapCreateRequest request = new ScrapCreateRequest(null, " image memo ", " " + imageObjectKey + " ");
        String normalizedRawContent = contentHashPolicy.normalize(" image memo ");
        String contentHash = contentHashPolicy.createContentHash(normalizedRawContent);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(scrapRepository.findByUser_UserIdAndSourceTypeAndContentHash(userId, SourceType.IMAGE, contentHash))
                .thenReturn(Optional.empty());
        when(scrapRepository.save(any(Scrap.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(knowledgeCardRepository.findByScrapIdWithCategory(any(UUID.class))).thenReturn(Optional.empty());
        when(asyncJobRepository.findByTargetIdAndJobType(any(UUID.class), eq(JobType.CARD_ANALYSIS)))
                .thenReturn(List.of());
        UUID refineJobId = UUID.randomUUID();
        UUID analysisJobId = UUID.randomUUID();
        when(asyncJobManager.enqueue(eq(JobType.SCRAP_REFINE), any(UUID.class))).thenReturn(refineJobId);
        when(asyncJobManager.enqueue(eq(JobType.CARD_ANALYSIS), any(UUID.class))).thenReturn(analysisJobId);

        ScrapResponse response = scrapService.createScrap(userId, request);

        ArgumentCaptor<Scrap> captor = ArgumentCaptor.forClass(Scrap.class);
        verify(scrapRepository).save(captor.capture());
        assertThat(captor.getValue().getSourceType()).isEqualTo(SourceType.IMAGE);
        assertThat(captor.getValue().getImageObjectKey()).isEqualTo(imageObjectKey);
        assertThat(response.sourceType()).isEqualTo(SourceType.IMAGE);
        assertThat(response.imageObjectKey()).isEqualTo(imageObjectKey);
        assertThat(response.analysisJobId()).isEqualTo(analysisJobId);
        assertThat(response.refineJobId()).isEqualTo(refineJobId);
        assertThat(response.originStatus()).isEqualTo(ScrapOriginStatus.CREATED);
        assertThat(response.refineStatus()).isEqualTo(ScrapRefineStatus.REFINE_IN_PROGRESS);
        assertThat(response.cardCreationStatus()).isEqualTo(ScrapCardCreationStatus.ANALYSIS_IN_PROGRESS);
        verify(asyncJobManager).enqueue(JobType.SCRAP_REFINE, captor.getValue().getScrapId());
    }

    @Test
    void createScrap_returnsExistingScrapWithNewJobWhenSameSourceExists() {
        UUID userId = UUID.randomUUID();
        User user = buildUser(userId);
        String contentHash = contentHashPolicy.createContentHash("hello");
        Scrap existingScrap = Scrap.builder()
                .user(user)
                .sourceType(SourceType.TEXT)
                .rawContent("hello")
                .contentHash(contentHash)
                .build();
        ScrapCreateRequest request = new ScrapCreateRequest(null, " hello ", null);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(scrapRepository.findByUser_UserIdAndSourceTypeAndContentHash(userId, SourceType.TEXT, contentHash))
                .thenReturn(Optional.of(existingScrap));
        UUID jobId = UUID.randomUUID();
        UUID refineJobId = UUID.randomUUID();
        when(knowledgeCardRepository.findByScrapIdWithCategory(existingScrap.getScrapId())).thenReturn(Optional.empty());
        when(asyncJobRepository.findByTargetIdAndJobType(existingScrap.getScrapId(), JobType.CARD_ANALYSIS))
                .thenReturn(List.of());
        when(asyncJobManager.enqueue(JobType.SCRAP_REFINE, existingScrap.getScrapId())).thenReturn(refineJobId);
        when(asyncJobManager.enqueue(JobType.CARD_ANALYSIS, existingScrap.getScrapId())).thenReturn(jobId);

        ScrapResponse response = scrapService.createScrap(userId, request);

        verify(scrapRepository, never()).save(any(Scrap.class));
        verify(asyncJobManager).enqueue(JobType.SCRAP_REFINE, existingScrap.getScrapId());
        assertThat(response.scrapId()).isEqualTo(existingScrap.getScrapId());
        assertThat(response.analysisJobId()).isEqualTo(jobId);
        assertThat(response.refineJobId()).isEqualTo(refineJobId);
        assertThat(response.originStatus()).isEqualTo(ScrapOriginStatus.EXISTING);
        assertThat(response.refineStatus()).isEqualTo(ScrapRefineStatus.REFINE_IN_PROGRESS);
        assertThat(response.cardCreationStatus()).isEqualTo(ScrapCardCreationStatus.ANALYSIS_IN_PROGRESS);
    }

    @Test
    void createScrap_doesNotRegisterRefineJobWhenExistingScrapHasRefinedContent() {
        UUID userId = UUID.randomUUID();
        User user = buildUser(userId);
        String contentHash = contentHashPolicy.createContentHash("hello");
        Scrap existingScrap = Scrap.builder()
                .user(user)
                .sourceType(SourceType.TEXT)
                .rawContent("hello")
                .refinedContent("refined hello")
                .contentHash(contentHash)
                .build();
        UUID jobId = UUID.randomUUID();
        ScrapCreateRequest request = new ScrapCreateRequest(null, " hello ", null);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(scrapRepository.findByUser_UserIdAndSourceTypeAndContentHash(userId, SourceType.TEXT, contentHash))
                .thenReturn(Optional.of(existingScrap));
        when(knowledgeCardRepository.findByScrapIdWithCategory(existingScrap.getScrapId())).thenReturn(Optional.empty());
        when(asyncJobRepository.findByTargetIdAndJobType(existingScrap.getScrapId(), JobType.CARD_ANALYSIS))
                .thenReturn(List.of());
        when(asyncJobManager.enqueue(JobType.CARD_ANALYSIS, existingScrap.getScrapId())).thenReturn(jobId);

        ScrapResponse response = scrapService.createScrap(userId, request);

        verify(asyncJobManager, never()).enqueue(JobType.SCRAP_REFINE, existingScrap.getScrapId());
        assertThat(response.analysisJobId()).isEqualTo(jobId);
        assertThat(response.refineJobId()).isNull();
        assertThat(response.originStatus()).isEqualTo(ScrapOriginStatus.EXISTING);
        assertThat(response.refineStatus()).isEqualTo(ScrapRefineStatus.REFINE_COMPLETED);
        assertThat(response.cardCreationStatus()).isEqualTo(ScrapCardCreationStatus.ANALYSIS_IN_PROGRESS);
    }

    @Test
    void createScrap_returnsActiveJobWhenSameSourceJobExists() {
        UUID userId = UUID.randomUUID();
        User user = buildUser(userId);
        String contentHash = contentHashPolicy.createContentHash("hello");
        Scrap existingScrap = Scrap.builder()
                .user(user)
                .sourceType(SourceType.TEXT)
                .rawContent("hello")
                .contentHash(contentHash)
                .build();
        UUID jobId = UUID.randomUUID();
        AsyncJob activeJob = buildJob(jobId, JobType.CARD_ANALYSIS, JobStatus.PROCESSING, existingScrap.getScrapId());
        ScrapCreateRequest request = new ScrapCreateRequest(null, " hello ", null);
        UUID refineJobId = UUID.randomUUID();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(scrapRepository.findByUser_UserIdAndSourceTypeAndContentHash(userId, SourceType.TEXT, contentHash))
                .thenReturn(Optional.of(existingScrap));
        when(knowledgeCardRepository.findByScrapIdWithCategory(existingScrap.getScrapId())).thenReturn(Optional.empty());
        when(asyncJobRepository.findByTargetIdAndJobType(existingScrap.getScrapId(), JobType.CARD_ANALYSIS))
                .thenReturn(List.of(activeJob));
        when(asyncJobManager.enqueue(JobType.SCRAP_REFINE, existingScrap.getScrapId())).thenReturn(refineJobId);

        ScrapResponse response = scrapService.createScrap(userId, request);

        verify(asyncJobManager).enqueue(JobType.SCRAP_REFINE, existingScrap.getScrapId());
        verify(asyncJobManager, never()).enqueue(JobType.CARD_ANALYSIS, existingScrap.getScrapId());
        assertThat(response.analysisJobId()).isEqualTo(jobId);
        assertThat(response.refineJobId()).isEqualTo(refineJobId);
        assertThat(response.originStatus()).isEqualTo(ScrapOriginStatus.EXISTING);
        assertThat(response.refineStatus()).isEqualTo(ScrapRefineStatus.REFINE_IN_PROGRESS);
        assertThat(response.cardCreationStatus()).isEqualTo(ScrapCardCreationStatus.ANALYSIS_IN_PROGRESS);
    }

    @Test
    void createScrap_returnsCreatedCardWhenCardAlreadyExists() {
        UUID userId = UUID.randomUUID();
        User user = buildUser(userId);
        String contentHash = contentHashPolicy.createContentHash("hello");
        Scrap existingScrap = Scrap.builder()
                .user(user)
                .sourceType(SourceType.TEXT)
                .rawContent("hello")
                .contentHash(contentHash)
                .build();
        UUID cardId = UUID.randomUUID();
        UUID refineJobId = UUID.randomUUID();
        KnowledgeCard card = buildCard(cardId, existingScrap, user);
        ScrapCreateRequest request = new ScrapCreateRequest(null, " hello ", null);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(scrapRepository.findByUser_UserIdAndSourceTypeAndContentHash(userId, SourceType.TEXT, contentHash))
                .thenReturn(Optional.of(existingScrap));
        when(knowledgeCardRepository.findByScrapIdWithCategory(existingScrap.getScrapId()))
                .thenReturn(Optional.of(card));
        when(asyncJobManager.enqueue(JobType.SCRAP_REFINE, existingScrap.getScrapId())).thenReturn(refineJobId);

        ScrapResponse response = scrapService.createScrap(userId, request);

        verify(asyncJobManager).enqueue(JobType.SCRAP_REFINE, existingScrap.getScrapId());
        verify(asyncJobManager, never()).enqueue(JobType.CARD_ANALYSIS, existingScrap.getScrapId());
        assertThat(response.analysisJobId()).isNull();
        assertThat(response.refineJobId()).isEqualTo(refineJobId);
        assertThat(response.cardId()).isEqualTo(cardId);
        assertThat(response.originStatus()).isEqualTo(ScrapOriginStatus.EXISTING);
        assertThat(response.refineStatus()).isEqualTo(ScrapRefineStatus.REFINE_IN_PROGRESS);
        assertThat(response.cardCreationStatus()).isEqualTo(ScrapCardCreationStatus.CARD_READY);
    }

    @Test
    void createScrap_returnsExistingScrapWhenUniqueConstraintConflicts() {
        UUID userId = UUID.randomUUID();
        User user = buildUser(userId);
        String contentHash = contentHashPolicy.createContentHash("hello");
        Scrap existingScrap = Scrap.builder()
                .user(user)
                .sourceType(SourceType.TEXT)
                .rawContent("hello")
                .contentHash(contentHash)
                .build();
        UUID jobId = UUID.randomUUID();
        UUID refineJobId = UUID.randomUUID();
        ScrapCreateRequest request = new ScrapCreateRequest(null, " hello ", null);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(scrapRepository.findByUser_UserIdAndSourceTypeAndContentHash(userId, SourceType.TEXT, contentHash))
                .thenReturn(Optional.empty(), Optional.of(existingScrap));
        when(scrapRepository.save(any(Scrap.class))).thenThrow(new DataIntegrityViolationException("duplicate"));
        when(knowledgeCardRepository.findByScrapIdWithCategory(existingScrap.getScrapId())).thenReturn(Optional.empty());
        when(asyncJobRepository.findByTargetIdAndJobType(existingScrap.getScrapId(), JobType.CARD_ANALYSIS))
                .thenReturn(List.of());
        when(asyncJobManager.enqueue(JobType.SCRAP_REFINE, existingScrap.getScrapId())).thenReturn(refineJobId);
        when(asyncJobManager.enqueue(JobType.CARD_ANALYSIS, existingScrap.getScrapId())).thenReturn(jobId);

        ScrapResponse response = scrapService.createScrap(userId, request);

        assertThat(response.scrapId()).isEqualTo(existingScrap.getScrapId());
        assertThat(response.analysisJobId()).isEqualTo(jobId);
        assertThat(response.refineJobId()).isEqualTo(refineJobId);
        assertThat(response.originStatus()).isEqualTo(ScrapOriginStatus.EXISTING);
        assertThat(response.refineStatus()).isEqualTo(ScrapRefineStatus.REFINE_IN_PROGRESS);
        assertThat(response.cardCreationStatus()).isEqualTo(ScrapCardCreationStatus.ANALYSIS_IN_PROGRESS);
        verify(asyncJobManager).enqueue(JobType.SCRAP_REFINE, existingScrap.getScrapId());
    }

    @Test
    void createScrap_returnsActiveJobWhenEnqueueConflicts() {
        UUID userId = UUID.randomUUID();
        User user = buildUser(userId);
        String contentHash = contentHashPolicy.createContentHash("hello");
        Scrap savedScrap = Scrap.builder()
                .user(user)
                .sourceType(SourceType.TEXT)
                .rawContent("hello")
                .contentHash(contentHash)
                .build();
        UUID jobId = UUID.randomUUID();
        UUID refineJobId = UUID.randomUUID();
        AsyncJob activeJob = buildJob(jobId, JobType.CARD_ANALYSIS, JobStatus.PENDING, savedScrap.getScrapId());
        ScrapCreateRequest request = new ScrapCreateRequest(null, " hello ", null);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(scrapRepository.findByUser_UserIdAndSourceTypeAndContentHash(userId, SourceType.TEXT, contentHash))
                .thenReturn(Optional.empty());
        when(scrapRepository.save(any(Scrap.class))).thenReturn(savedScrap);
        when(knowledgeCardRepository.findByScrapIdWithCategory(savedScrap.getScrapId())).thenReturn(Optional.empty());
        when(asyncJobRepository.findByTargetIdAndJobType(savedScrap.getScrapId(), JobType.CARD_ANALYSIS))
                .thenReturn(List.of(), List.of(activeJob));
        when(asyncJobManager.enqueue(JobType.SCRAP_REFINE, savedScrap.getScrapId())).thenReturn(refineJobId);
        when(asyncJobManager.enqueue(JobType.CARD_ANALYSIS, savedScrap.getScrapId()))
                .thenThrow(new BusinessException(CommonErrorCode.DUPLICATE_RESOURCE));

        ScrapResponse response = scrapService.createScrap(userId, request);

        assertThat(response.analysisJobId()).isEqualTo(jobId);
        assertThat(response.refineJobId()).isEqualTo(refineJobId);
        assertThat(response.originStatus()).isEqualTo(ScrapOriginStatus.CREATED);
        assertThat(response.refineStatus()).isEqualTo(ScrapRefineStatus.REFINE_IN_PROGRESS);
        assertThat(response.cardCreationStatus()).isEqualTo(ScrapCardCreationStatus.ANALYSIS_IN_PROGRESS);
    }

    @Test
    void createScrap_ignoresScrapRefineDuplicateWhenActiveJobExists() {
        UUID userId = UUID.randomUUID();
        User user = buildUser(userId);
        String contentHash = contentHashPolicy.createContentHash("hello");
        Scrap savedScrap = Scrap.builder()
                .user(user)
                .sourceType(SourceType.TEXT)
                .rawContent("hello")
                .contentHash(contentHash)
                .build();
        UUID cardAnalysisJobId = UUID.randomUUID();
        UUID refineJobId = UUID.randomUUID();
        AsyncJob activeRefineJob = buildJob(refineJobId, JobType.SCRAP_REFINE, JobStatus.PENDING, savedScrap.getScrapId());
        ScrapCreateRequest request = new ScrapCreateRequest(null, " hello ", null);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(scrapRepository.findByUser_UserIdAndSourceTypeAndContentHash(userId, SourceType.TEXT, contentHash))
                .thenReturn(Optional.empty());
        when(scrapRepository.save(any(Scrap.class))).thenReturn(savedScrap);
        when(asyncJobManager.enqueue(JobType.SCRAP_REFINE, savedScrap.getScrapId()))
                .thenThrow(new BusinessException(CommonErrorCode.DUPLICATE_RESOURCE));
        when(asyncJobRepository.findByTargetIdAndJobType(savedScrap.getScrapId(), JobType.SCRAP_REFINE))
                .thenReturn(List.of(activeRefineJob));
        when(knowledgeCardRepository.findByScrapIdWithCategory(savedScrap.getScrapId())).thenReturn(Optional.empty());
        when(asyncJobRepository.findByTargetIdAndJobType(savedScrap.getScrapId(), JobType.CARD_ANALYSIS))
                .thenReturn(List.of());
        when(asyncJobManager.enqueue(JobType.CARD_ANALYSIS, savedScrap.getScrapId()))
                .thenReturn(cardAnalysisJobId);

        ScrapResponse response = scrapService.createScrap(userId, request);

        assertThat(response.scrapId()).isEqualTo(savedScrap.getScrapId());
        assertThat(response.analysisJobId()).isEqualTo(cardAnalysisJobId);
        assertThat(response.refineJobId()).isEqualTo(refineJobId);
        assertThat(response.originStatus()).isEqualTo(ScrapOriginStatus.CREATED);
        assertThat(response.refineStatus()).isEqualTo(ScrapRefineStatus.REFINE_IN_PROGRESS);
        assertThat(response.cardCreationStatus()).isEqualTo(ScrapCardCreationStatus.ANALYSIS_IN_PROGRESS);
    }

    private User buildUser(UUID userId) {
        User user = User.builder()
                .username("testuser")
                .provider(AuthProvider.LOCAL)
                .build();
        ReflectionTestUtils.setField(user, "userId", userId);
        return user;
    }

    private AsyncJob buildJob(UUID jobId, JobType jobType, JobStatus status, UUID targetId) {
        AsyncJob job = AsyncJob.builder()
                .jobType(jobType)
                .targetId(targetId)
                .build();
        ReflectionTestUtils.setField(job, "jobId", jobId);
        job.updateStatus(status);
        return job;
    }

    private KnowledgeCard buildCard(UUID cardId, Scrap scrap, User user) {
        Category category = Category.builder()
                .user(user)
                .categoryName("테스트")
                .build();
        KnowledgeCard card = KnowledgeCard.builder()
                .scrap(scrap)
                .category(category)
                .title("테스트 카드")
                .summary("요약")
                .build();
        ReflectionTestUtils.setField(card, "cardId", cardId);
        return card;
    }
}
