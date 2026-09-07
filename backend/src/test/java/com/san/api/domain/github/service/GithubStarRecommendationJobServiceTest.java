package com.san.api.domain.github.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.san.api.domain.github.dto.response.GithubStarRecommendationJobResponse;
import com.san.api.domain.github.dto.response.GithubStarRecommendationListResponse;
import com.san.api.domain.github.entity.GithubStarRecommendation;
import com.san.api.domain.github.repository.GithubStarRecommendationRepository;
import com.san.api.domain.user.entity.AuthProvider;
import com.san.api.domain.user.entity.User;
import com.san.api.global.async.entity.AsyncJob;
import com.san.api.global.async.entity.JobStatus;
import com.san.api.global.async.entity.JobType;
import com.san.api.global.async.repository.AsyncJobRepository;
import com.san.api.global.async.service.AsyncJobManager;
import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.CommonErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GithubStarRecommendationJobServiceTest {

    private GithubStarRecommendationRepository githubStarRecommendationRepository;
    private AsyncJobManager asyncJobManager;
    private AsyncJobRepository asyncJobRepository;
    private GithubStarRecommendationJobService service;

    @BeforeEach
    void setUp() {
        githubStarRecommendationRepository = mock(GithubStarRecommendationRepository.class);
        asyncJobManager = mock(AsyncJobManager.class);
        asyncJobRepository = mock(AsyncJobRepository.class);
        service = new GithubStarRecommendationJobService(
                githubStarRecommendationRepository,
                asyncJobManager,
                asyncJobRepository,
                new ObjectMapper()
        );
    }

    @Test
    void requestRecommendation_enqueuesJobWhenRecommendationDoesNotExist() {
        UUID userId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();

        when(githubStarRecommendationRepository.findAllByUser_UserIdOrderByCreatedAtDesc(userId))
                .thenReturn(List.of());
        when(asyncJobRepository.findByTargetIdAndJobType(userId, JobType.GITHUB_STAR_RECOMMENDATION))
                .thenReturn(List.of());
        when(asyncJobManager.enqueue(JobType.GITHUB_STAR_RECOMMENDATION, userId)).thenReturn(jobId);

        GithubStarRecommendationJobResponse response = service.requestRecommendation(userId);

        assertThat(response.jobId()).isEqualTo(jobId);
        assertThat(response.alreadyRecommended()).isFalse();
        assertThat(response.recommendations()).isEmpty();
        verify(asyncJobManager).enqueue(JobType.GITHUB_STAR_RECOMMENDATION, userId);
    }

    @Test
    void requestRecommendation_returnsActiveJobWithoutEnqueue() {
        UUID userId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        AsyncJob activeJob = buildJob(jobId, JobStatus.PROCESSING, userId);

        when(githubStarRecommendationRepository.findAllByUser_UserIdOrderByCreatedAtDesc(userId))
                .thenReturn(List.of());
        when(asyncJobRepository.findByTargetIdAndJobType(userId, JobType.GITHUB_STAR_RECOMMENDATION))
                .thenReturn(List.of(activeJob));

        GithubStarRecommendationJobResponse response = service.requestRecommendation(userId);

        assertThat(response.jobId()).isEqualTo(jobId);
        assertThat(response.alreadyRecommended()).isFalse();
        verify(asyncJobManager, never()).enqueue(JobType.GITHUB_STAR_RECOMMENDATION, userId);
    }

    @Test
    void requestRecommendation_returnsActiveJobWhenEnqueueDuplicated() {
        UUID userId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        AsyncJob activeJob = buildJob(jobId, JobStatus.PENDING, userId);

        when(githubStarRecommendationRepository.findAllByUser_UserIdOrderByCreatedAtDesc(userId))
                .thenReturn(List.of());
        when(asyncJobRepository.findByTargetIdAndJobType(userId, JobType.GITHUB_STAR_RECOMMENDATION))
                .thenReturn(List.of(), List.of(activeJob));
        when(asyncJobManager.enqueue(JobType.GITHUB_STAR_RECOMMENDATION, userId))
                .thenThrow(new BusinessException(CommonErrorCode.DUPLICATE_RESOURCE));

        GithubStarRecommendationJobResponse response = service.requestRecommendation(userId);

        assertThat(response.jobId()).isEqualTo(jobId);
        assertThat(response.alreadyRecommended()).isFalse();
        verify(asyncJobManager).enqueue(JobType.GITHUB_STAR_RECOMMENDATION, userId);
    }

    @Test
    void requestRecommendation_returnsExistingRecommendationsWithoutEnqueue() {
        UUID userId = UUID.randomUUID();
        GithubStarRecommendation recommendation = buildRecommendation();

        when(githubStarRecommendationRepository.findAllByUser_UserIdOrderByCreatedAtDesc(userId))
                .thenReturn(List.of(recommendation));

        GithubStarRecommendationJobResponse response = service.requestRecommendation(userId);

        assertThat(response.jobId()).isNull();
        assertThat(response.alreadyRecommended()).isTrue();
        assertThat(response.recommendations()).hasSize(1);
        assertThat(response.recommendations().get(0).recommendationUrl()).isEqualTo("https://example.com");
        assertThat(response.recommendations().get(0).tagList()).containsExactly("Spring");
        verify(asyncJobManager, never()).enqueue(JobType.GITHUB_STAR_RECOMMENDATION, userId);
    }

    @Test
    void getRecommendations_returnsUserRecommendations() {
        UUID userId = UUID.randomUUID();
        GithubStarRecommendation recommendation = buildRecommendation();

        when(githubStarRecommendationRepository.findAllByUser_UserIdOrderByCreatedAtDesc(userId))
                .thenReturn(List.of(recommendation));

        GithubStarRecommendationListResponse response = service.getRecommendations(userId);

        assertThat(response.recommendations()).hasSize(1);
        assertThat(response.recommendations().get(0).title()).isEqualTo("title");
    }

    private GithubStarRecommendation buildRecommendation() {
        User user = User.builder()
                .username("user@example.com")
                .provider(AuthProvider.LOCAL)
                .build();
        return new GithubStarRecommendation(
                user,
                UUID.randomUUID(),
                "https://example.com",
                "title",
                "summary",
                "{\"title\":\"title\",\"summary\":\"summary\",\"tags\":[\"Spring\"],\"category\":\"Backend\",\"embedding\":[0.1]}"
        );
    }

    private AsyncJob buildJob(UUID jobId, JobStatus status, UUID targetId) {
        AsyncJob job = AsyncJob.builder()
                .jobType(JobType.GITHUB_STAR_RECOMMENDATION)
                .targetId(targetId)
                .build();
        ReflectionTestUtils.setField(job, "jobId", jobId);
        job.updateStatus(status);
        return job;
    }
}
