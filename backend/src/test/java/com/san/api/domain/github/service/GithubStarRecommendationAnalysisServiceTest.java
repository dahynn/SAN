package com.san.api.domain.github.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.san.api.domain.github.entity.GithubStarRecommendation;
import com.san.api.domain.github.repository.GithubStarRecommendationRepository;
import com.san.api.domain.user.entity.AuthProvider;
import com.san.api.domain.user.entity.User;
import com.san.api.domain.user.repository.UserRepository;
import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.CommonErrorCode;
import com.san.api.global.external.ai.client.AiAnalysisClient;
import com.san.api.global.external.ai.dto.request.AiAnalyzeRequest;
import com.san.api.global.external.ai.dto.response.AiAnalyzeResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GithubStarRecommendationAnalysisServiceTest {

    private UserRepository userRepository;
    private GithubStarRecommendationService githubStarRecommendationService;
    private GithubStarRecommendationRepository githubStarRecommendationRepository;
    private AiAnalysisClient aiAnalysisClient;
    private GithubStarRecommendationAnalysisService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        githubStarRecommendationService = mock(GithubStarRecommendationService.class);
        githubStarRecommendationRepository = mock(GithubStarRecommendationRepository.class);
        aiAnalysisClient = mock(AiAnalysisClient.class);
        service = new GithubStarRecommendationAnalysisService(
                userRepository,
                githubStarRecommendationService,
                githubStarRecommendationRepository,
                aiAnalysisClient,
                new ObjectMapper()
        );
    }

    @Test
    void analyzeAndSave_analyzesRecommendedUrlsAndSavesRecommendations() {
        UUID userId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        User user = User.builder()
                .username("user@example.com")
                .provider(AuthProvider.LOCAL)
                .build();
        AiAnalyzeRequest request = new AiAnalyzeRequest("url", " https://example.com/article ");
        AiAnalyzeResponse analysis = new AiAnalyzeResponse(
                " Title ",
                " Summary ",
                List.of("java"),
                "Backend",
                new float[]{0.1f}
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(githubStarRecommendationService.recommendAnalyzeInputs(userId)).thenReturn(List.of(request));
        when(aiAnalysisClient.analyze(request)).thenReturn(analysis);
        when(githubStarRecommendationRepository.saveAll(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        List<GithubStarRecommendation> result = service.analyzeAndSave(userId, jobId);

        assertThat(result).hasSize(1);
        GithubStarRecommendation recommendation = result.get(0);
        assertThat(recommendation.getUser()).isEqualTo(user);
        assertThat(recommendation.getJobId()).isEqualTo(jobId);
        assertThat(recommendation.getUrl()).isEqualTo("https://example.com/article");
        assertThat(recommendation.getTitle()).isEqualTo("Title");
        assertThat(recommendation.getSummary()).isEqualTo("Summary");
        assertThat(recommendation.getAnalysisResult()).contains("\"title\":\" Title \"");
        verify(githubStarRecommendationRepository).saveAll(result);
    }

    @Test
    void analyzeAndSave_throwsWhenUserDoesNotExist() {
        UUID userId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.analyzeAndSave(userId, jobId))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(CommonErrorCode.RESOURCE_NOT_FOUND));
    }
}
