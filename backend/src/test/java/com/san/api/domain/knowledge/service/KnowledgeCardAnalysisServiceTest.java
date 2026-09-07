package com.san.api.domain.knowledge.service;

import com.san.api.domain.knowledge.entity.Category;
import com.san.api.domain.knowledge.entity.KnowledgeCard;
import com.san.api.domain.knowledge.repository.CardTagRepository;
import com.san.api.domain.knowledge.repository.CategoryRepository;
import com.san.api.domain.knowledge.repository.KnowledgeCardRepository;
import com.san.api.domain.knowledge.repository.TagRepository;
import com.san.api.domain.scrap.entity.Scrap;
import com.san.api.domain.scrap.entity.SourceType;
import com.san.api.domain.scrap.repository.ScrapRepository;
import com.san.api.domain.user.entity.AuthProvider;
import com.san.api.domain.user.entity.User;
import com.san.api.global.external.ai.client.AiAnalysisClient;
import com.san.api.global.external.ai.dto.request.AiAnalyzeRequest;
import com.san.api.global.external.ai.dto.response.AiAnalyzeResponse;
import com.san.api.global.external.s3.service.S3PresignedUrlService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KnowledgeCardAnalysisServiceTest {

    @Mock
    private ScrapRepository scrapRepository;
    @Mock
    private KnowledgeCardRepository knowledgeCardRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private TagRepository tagRepository;
    @Mock
    private CardTagRepository cardTagRepository;
    @Mock
    private AiAnalysisClient aiAnalysisClient;
    @Mock
    private S3PresignedUrlService s3PresignedUrlService;

    @InjectMocks
    private KnowledgeCardAnalysisService knowledgeCardAnalysisService;

    @Test
    void createKnowledgeCard_convertsImageObjectKeyToPresignedUrl() {
        UUID userId = UUID.randomUUID();
        User user = buildUser(userId);
        String imageObjectKey = "scrap/images/%s/image.png".formatted(userId);
        String presignedUrl = "https://bucket.s3.us-east-1.amazonaws.com/scrap/images/image.png?signature=test";
        Scrap scrap = Scrap.builder()
                .user(user)
                .sourceType(SourceType.IMAGE)
                .rawContent("fallback")
                .imageObjectKey(imageObjectKey)
                .build();
        Category category = Category.builder()
                .user(user)
                .categoryName("Study")
                .build();

        when(scrapRepository.findById(scrap.getScrapId())).thenReturn(Optional.of(scrap));
        when(knowledgeCardRepository.existsByScrap_ScrapId(scrap.getScrapId())).thenReturn(false);
        when(s3PresignedUrlService.createDownloadPresignedUrl(imageObjectKey)).thenReturn(presignedUrl);
        when(aiAnalysisClient.analyze(any(AiAnalyzeRequest.class)))
                .thenReturn(new AiAnalyzeResponse("Title", "Summary", List.of(), "Study", new float[]{0.1f}));
        when(categoryRepository.findByUser_UserIdAndCategoryName(userId, "Study")).thenReturn(Optional.of(category));
        when(knowledgeCardRepository.saveAndFlush(any(KnowledgeCard.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        knowledgeCardAnalysisService.createKnowledgeCard(scrap.getScrapId());

        ArgumentCaptor<AiAnalyzeRequest> captor = ArgumentCaptor.forClass(AiAnalyzeRequest.class);
        verify(aiAnalysisClient).analyze(captor.capture());
        assertThat(captor.getValue().inputType()).isEqualTo("image");
        assertThat(captor.getValue().content()).isEqualTo(presignedUrl);
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
