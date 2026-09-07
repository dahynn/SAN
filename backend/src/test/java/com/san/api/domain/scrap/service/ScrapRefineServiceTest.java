package com.san.api.domain.scrap.service;

import com.san.api.domain.scrap.entity.Scrap;
import com.san.api.domain.scrap.entity.SourceType;
import com.san.api.domain.scrap.repository.ScrapRepository;
import com.san.api.domain.user.entity.AuthProvider;
import com.san.api.domain.user.entity.User;
import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.CommonErrorCode;
import com.san.api.global.external.ai.client.AiScrapRefineClient;
import com.san.api.global.external.ai.dto.request.AiScrapRefineRequest;
import com.san.api.global.external.ai.dto.response.AiScrapRefineResponse;
import com.san.api.global.external.s3.service.S3PresignedUrlService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScrapRefineServiceTest {

    @Mock
    private ScrapRepository scrapRepository;

    @Mock
    private AiScrapRefineClient aiScrapRefineClient;

    @Mock
    private S3PresignedUrlService s3PresignedUrlService;

    @InjectMocks
    private ScrapRefineService scrapRefineService;

    @Test
    void refine_textSource_updatesRefinedContent() {
        UUID scrapId = UUID.randomUUID();
        Scrap scrap = buildScrap(SourceType.TEXT, null, "raw content", null);

        when(scrapRepository.findById(scrapId)).thenReturn(Optional.of(scrap));
        when(aiScrapRefineClient.refine(org.mockito.ArgumentMatchers.any(AiScrapRefineRequest.class)))
                .thenReturn(new AiScrapRefineResponse("refined content"));

        scrapRefineService.refine(scrapId);

        ArgumentCaptor<AiScrapRefineRequest> captor = ArgumentCaptor.forClass(AiScrapRefineRequest.class);
        verify(aiScrapRefineClient).refine(captor.capture());
        assertThat(captor.getValue().inputType()).isEqualTo("text");
        assertThat(captor.getValue().content()).isEqualTo("raw content");
        assertThat(scrap.getRefinedContent()).isEqualTo("refined content");
    }

    @Test
    void refine_linkSource_usesSourceUrlFirst() {
        UUID scrapId = UUID.randomUUID();
        Scrap scrap = buildScrap(SourceType.LINK, "https://example.com/article", "raw content", null);

        when(scrapRepository.findById(scrapId)).thenReturn(Optional.of(scrap));
        when(aiScrapRefineClient.refine(org.mockito.ArgumentMatchers.any(AiScrapRefineRequest.class)))
                .thenReturn(new AiScrapRefineResponse("refined content"));

        scrapRefineService.refine(scrapId);

        ArgumentCaptor<AiScrapRefineRequest> captor = ArgumentCaptor.forClass(AiScrapRefineRequest.class);
        verify(aiScrapRefineClient).refine(captor.capture());
        assertThat(captor.getValue().inputType()).isEqualTo("url");
        assertThat(captor.getValue().content()).isEqualTo("https://example.com/article");
        assertThat(scrap.getRefinedContent()).isEqualTo("refined content");
    }

    @Test
    void refine_imageSource_usesPresignedUrl() {
        UUID scrapId = UUID.randomUUID();
        Scrap scrap = buildScrap(SourceType.IMAGE, null, "raw content", "images/test.png");

        when(scrapRepository.findById(scrapId)).thenReturn(Optional.of(scrap));
        when(s3PresignedUrlService.createDownloadPresignedUrl("images/test.png"))
                .thenReturn("https://cdn.example.com/images/test.png");
        when(aiScrapRefineClient.refine(org.mockito.ArgumentMatchers.any(AiScrapRefineRequest.class)))
                .thenReturn(new AiScrapRefineResponse("refined content"));

        scrapRefineService.refine(scrapId);

        ArgumentCaptor<AiScrapRefineRequest> captor = ArgumentCaptor.forClass(AiScrapRefineRequest.class);
        verify(aiScrapRefineClient).refine(captor.capture());
        assertThat(captor.getValue().inputType()).isEqualTo("image");
        assertThat(captor.getValue().content()).isEqualTo("https://cdn.example.com/images/test.png");
        assertThat(scrap.getRefinedContent()).isEqualTo("refined content");
    }

    @Test
    void refine_notFound_throwsException() {
        UUID scrapId = UUID.randomUUID();
        when(scrapRepository.findById(scrapId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> scrapRefineService.refine(scrapId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", CommonErrorCode.RESOURCE_NOT_FOUND);
        verifyNoInteractions(aiScrapRefineClient);
    }

    private Scrap buildScrap(SourceType sourceType, String sourceUrl, String rawContent, String imageObjectKey) {
        User user = User.builder()
                .username("testuser")
                .provider(AuthProvider.LOCAL)
                .build();

        return Scrap.builder()
                .user(user)
                .sourceType(sourceType)
                .sourceUrl(sourceUrl)
                .rawContent(rawContent)
                .contentHash("hash")
                .imageObjectKey(imageObjectKey)
                .build();
    }
}
