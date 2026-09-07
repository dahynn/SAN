package com.san.api.domain.github.entity;

import com.san.api.domain.user.entity.AuthProvider;
import com.san.api.domain.user.entity.User;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class GithubStarRecommendationTest {

    @Test
    void constructor_createsUncollectedRecommendation() {
        User user = createUser();
        UUID jobId = UUID.randomUUID();

        GithubStarRecommendation recommendation = new GithubStarRecommendation(
                user,
                jobId,
                "https://react.dev/learn/managing-state",
                "Managing State",
                "React state management summary",
                "{\"title\":\"Managing State\"}"
        );

        assertThat(recommendation.getGithubStarRecommendationId()).isNotNull();
        assertThat(recommendation.getUser()).isEqualTo(user);
        assertThat(recommendation.getJobId()).isEqualTo(jobId);
        assertThat(recommendation.getUrl()).isEqualTo("https://react.dev/learn/managing-state");
        assertThat(recommendation.getTitle()).isEqualTo("Managing State");
        assertThat(recommendation.getSummary()).isEqualTo("React state management summary");
        assertThat(recommendation.getAnalysisResult()).isEqualTo("{\"title\":\"Managing State\"}");
        assertThat(recommendation.isCollected()).isFalse();
        assertThat(recommendation.getCollectedTargetId()).isNull();
    }

    @Test
    void markCollected_updatesCollectionTarget() {
        GithubStarRecommendation recommendation = new GithubStarRecommendation(
                createUser(),
                UUID.randomUUID(),
                "https://docs.github.com/en/actions",
                "GitHub Actions",
                "GitHub Actions summary",
                "{\"title\":\"GitHub Actions\"}"
        );
        UUID collectedTargetId = UUID.randomUUID();

        recommendation.markCollected(collectedTargetId);

        assertThat(recommendation.isCollected()).isTrue();
        assertThat(recommendation.getCollectedTargetId()).isEqualTo(collectedTargetId);
    }

    private User createUser() {
        return User.builder()
                .username("user@example.com")
                .passwordHash("password")
                .provider(AuthProvider.LOCAL)
                .build();
    }
}
