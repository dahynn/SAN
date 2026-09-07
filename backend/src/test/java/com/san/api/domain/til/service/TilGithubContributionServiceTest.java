package com.san.api.domain.til.service;

import com.san.api.domain.github.entity.GithubRepositoryConnection;
import com.san.api.domain.til.dto.response.TilGithubContributionResponse;
import com.san.api.domain.til.entity.DailySummary;
import com.san.api.domain.til.entity.TilGithubCommit;
import com.san.api.domain.til.entity.TilGithubCommitStatus;
import com.san.api.domain.til.repository.TilGithubCommitRepository;
import com.san.api.domain.user.entity.AuthProvider;
import com.san.api.domain.user.entity.User;
import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.CommonErrorCode;
import com.san.api.global.external.github.dto.response.ExternalGithubRepositoryResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TilGithubContributionServiceTest {

    @Mock
    private TilGithubCommitRepository tilGithubCommitRepository;

    private TilGithubContributionService service;
    private UUID userId;
    private User user;
    private GithubRepositoryConnection repository;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-05-15T00:00:00Z"), ZoneId.of("Asia/Seoul"));
        service = new TilGithubContributionService(tilGithubCommitRepository, clock);

        user = User.builder()
                .username("til-user")
                .provider(AuthProvider.LOCAL)
                .build();
        userId = user.getUserId();
        repository = new GithubRepositoryConnection(user, new ExternalGithubRepositoryResponse(
                100L,
                "til",
                "octocat/til",
                false,
                "main",
                "https://github.com/octocat/til"
        ));
    }

    @Test
    void getContributions_returnsDailyGrassAndCommitDetails() {
        LocalDate from = LocalDate.of(2026, 5, 10);
        LocalDate to = LocalDate.of(2026, 5, 15);
        List<TilGithubCommit> commits = List.of(
                completedCommit("JPA", LocalDateTime.of(2026, 5, 12, 9, 30), "sha-1"),
                completedCommit("Spring", LocalDateTime.of(2026, 5, 12, 21, 0), "sha-2"),
                completedCommit("Security", LocalDateTime.of(2026, 5, 14, 10, 0), "sha-3"),
                completedCommit("Docker", LocalDateTime.of(2026, 5, 15, 8, 0), "sha-4")
        );
        when(tilGithubCommitRepository.findContributions(
                userId,
                TilGithubCommitStatus.COMPLETED,
                from.atStartOfDay(),
                to.plusDays(1).atStartOfDay(),
                null
        )).thenReturn(commits);

        TilGithubContributionResponse response = service.getContributions(userId, from, to, null);

        assertThat(response.totalCommits()).isEqualTo(4);
        assertThat(response.activeDays()).isEqualTo(3);
        assertThat(response.currentStreakDays()).isEqualTo(2);
        assertThat(response.longestStreakDays()).isEqualTo(2);
        assertThat(response.days()).hasSize(6);
        assertThat(response.days().get(2).date()).isEqualTo(LocalDate.of(2026, 5, 12));
        assertThat(response.days().get(2).count()).isEqualTo(2);
        assertThat(response.days().get(2).level()).isEqualTo(2);
        assertThat(response.repositories()).hasSize(1);
        assertThat(response.repositories().get(0).count()).isEqualTo(4);
        assertThat(response.commits()).extracting("title")
                .containsExactly("JPA", "Spring", "Security", "Docker");
    }

    @Test
    void getContributions_withoutDates_usesOneYearRangeEndingToday() {
        when(tilGithubCommitRepository.findContributions(
                userId,
                TilGithubCommitStatus.COMPLETED,
                LocalDate.of(2025, 5, 16).atStartOfDay(),
                LocalDate.of(2026, 5, 16).atStartOfDay(),
                100L
        )).thenReturn(List.of());

        TilGithubContributionResponse response = service.getContributions(userId, null, null, 100L);

        assertThat(response.from()).isEqualTo(LocalDate.of(2025, 5, 16));
        assertThat(response.to()).isEqualTo(LocalDate.of(2026, 5, 15));
        assertThat(response.days()).hasSize(365);
    }

    @Test
    void getContributions_withFutureToDate_limitsRangeToToday() {
        LocalDate from = LocalDate.of(2026, 5, 10);
        LocalDate today = LocalDate.of(2026, 5, 15);
        when(tilGithubCommitRepository.findContributions(
                userId,
                TilGithubCommitStatus.COMPLETED,
                from.atStartOfDay(),
                today.plusDays(1).atStartOfDay(),
                null
        )).thenReturn(List.of(completedCommit("Today", LocalDateTime.of(2026, 5, 15, 8, 0), "sha-today")));

        TilGithubContributionResponse response = service.getContributions(
                userId,
                from,
                LocalDate.of(2026, 5, 20),
                null
        );

        assertThat(response.to()).isEqualTo(today);
        assertThat(response.days()).hasSize(6);
        assertThat(response.currentStreakDays()).isEqualTo(1);
    }

    @Test
    void getContributions_withoutTodayCommit_keepsStreakThroughYesterday() {
        LocalDate from = LocalDate.of(2026, 5, 10);
        LocalDate today = LocalDate.of(2026, 5, 15);
        List<TilGithubCommit> commits = List.of(
                completedCommit("Yesterday", LocalDateTime.of(2026, 5, 14, 8, 0), "sha-yesterday"),
                completedCommit("BeforeYesterday", LocalDateTime.of(2026, 5, 13, 8, 0), "sha-before-yesterday")
        );
        when(tilGithubCommitRepository.findContributions(
                userId,
                TilGithubCommitStatus.COMPLETED,
                from.atStartOfDay(),
                today.plusDays(1).atStartOfDay(),
                null
        )).thenReturn(commits);

        TilGithubContributionResponse response = service.getContributions(userId, from, today, null);

        assertThat(response.currentStreakDays()).isEqualTo(2);
    }

    @Test
    void getContributions_withInvalidRange_fails() {
        assertThatThrownBy(() -> service.getContributions(
                userId,
                LocalDate.of(2026, 5, 16),
                LocalDate.of(2026, 5, 15),
                null
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(CommonErrorCode.INVALID_INPUT_VALUE);
    }

    private TilGithubCommit completedCommit(String title, LocalDateTime pushedAt, String sha) {
        DailySummary summary = DailySummary.builder()
                .user(user)
                .targetDate(pushedAt.toLocalDate())
                .title(title)
                .content("# " + title)
                .embedding(new float[]{0.1f})
                .build();
        TilGithubCommit commit = TilGithubCommit.builder()
                .dailySummary(summary)
                .githubRepositoryConnection(repository)
                .branch("main")
                .filePath("til/" + title + ".md")
                .title(title)
                .contentHash("hash-" + title)
                .commitMessage("docs: add " + title)
                .build();
        commit.markCompleted(sha, "https://github.com/octocat/til/commit/" + sha, pushedAt);
        return commit;
    }
}
