package com.san.api.domain.github.service;

import com.san.api.domain.github.entity.GithubAccount;
import com.san.api.domain.github.repository.GithubAccountRepository;
import com.san.api.domain.user.entity.AuthProvider;
import com.san.api.domain.user.entity.User;
import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.AuthErrorCode;
import com.san.api.global.external.ai.client.AiGithubStarRecommendationClient;
import com.san.api.global.external.ai.dto.request.AiAnalyzeRequest;
import com.san.api.global.external.ai.dto.request.AiGithubStarRecommendationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GithubStarRecommendationServiceTest {

    private GithubAccountRepository githubAccountRepository;
    private AiGithubStarRecommendationClient aiGithubStarRecommendationClient;
    private GithubStarRecommendationService service;

    @BeforeEach
    void setUp() {
        githubAccountRepository = mock(GithubAccountRepository.class);
        aiGithubStarRecommendationClient = mock(AiGithubStarRecommendationClient.class);
        service = new GithubStarRecommendationService(githubAccountRepository, aiGithubStarRecommendationClient);
        ReflectionTestUtils.setField(service, "recommendationLimit", 5);
    }

    @Test
    void recommendAnalyzeInputs_usesLinkedGithubUsernameAndConfiguredLimit() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .username("user@example.com")
                .passwordHash("password")
                .provider(AuthProvider.LOCAL)
                .build();
        GithubAccount githubAccount = new GithubAccount(user, "1", "octocat", "encrypted-token");
        List<AiAnalyzeRequest> recommendations = List.of(new AiAnalyzeRequest("url", "https://example.com"));

        when(githubAccountRepository.findByUser_UserId(userId)).thenReturn(Optional.of(githubAccount));
        when(aiGithubStarRecommendationClient.recommend(new AiGithubStarRecommendationRequest("octocat", 5)))
                .thenReturn(recommendations);

        List<AiAnalyzeRequest> result = service.recommendAnalyzeInputs(userId);

        assertThat(result).isEqualTo(recommendations);
        verify(aiGithubStarRecommendationClient).recommend(new AiGithubStarRecommendationRequest("octocat", 5));
    }

    @Test
    void recommendAnalyzeInputs_throwsWhenGithubAccountNotLinked() {
        UUID userId = UUID.randomUUID();
        when(githubAccountRepository.findByUser_UserId(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.recommendAnalyzeInputs(userId))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(AuthErrorCode.GITHUB_ACCOUNT_NOT_LINKED));
    }
}
