package com.san.api.domain.til.service;

import com.san.api.domain.github.entity.GithubAccount;
import com.san.api.domain.github.entity.GithubRepositoryConnection;
import com.san.api.domain.github.repository.GithubAccountRepository;
import com.san.api.domain.github.repository.GithubRepositoryConnectionRepository;
import com.san.api.domain.til.entity.DailySummary;
import com.san.api.domain.til.entity.TilGithubCommit;
import com.san.api.domain.til.entity.TilGithubCommitStatus;
import com.san.api.domain.til.repository.TilGithubCommitRepository;
import com.san.api.domain.user.entity.AuthProvider;
import com.san.api.domain.user.entity.User;
import com.san.api.global.async.entity.JobType;
import com.san.api.global.async.service.AsyncJobManager;
import com.san.api.global.audit.entity.AuditEventType;
import com.san.api.global.audit.entity.AuditTargetType;
import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.TilErrorCode;
import com.san.api.global.external.github.dto.response.ExternalGithubRepositoryResponse;
import com.san.api.global.security.crypto.AesGcmStringEncryptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** TIL GitHub 커밋 요청 검증과 작업 등록 정책 테스트. */
@ExtendWith(MockitoExtension.class)
class TilGithubCommitServiceTest {

    @Mock
    private DailySummaryService dailySummaryService;
    @Mock
    private GithubAccountRepository githubAccountRepository;
    @Mock
    private GithubRepositoryConnectionRepository githubRepositoryConnectionRepository;
    @Mock
    private TilGithubCommitRepository tilGithubCommitRepository;
    @Mock
    private TilGithubFilePolicy filePolicy;
    @Mock
    private TilGithubFilePathResolver filePathResolver;
    @Mock
    private AsyncJobManager asyncJobManager;
    @Mock
    private AesGcmStringEncryptor encryptor;
    @Mock
    private TilAuditService tilAuditService;

    private TilGithubCommitService service;
    private UUID userId;
    private UUID summaryId;
    private User user;
    private DailySummary summary;
    private GithubAccount githubAccount;
    private GithubRepositoryConnection repositoryConnection;

    @BeforeEach
    void setUp() {
        service = new TilGithubCommitService(
                dailySummaryService,
                githubAccountRepository,
                githubRepositoryConnectionRepository,
                tilGithubCommitRepository,
                filePolicy,
                filePathResolver,
                asyncJobManager,
                encryptor,
                tilAuditService
        );

        user = User.builder()
                .username("til-user")
                .provider(AuthProvider.LOCAL)
                .build();
        userId = user.getUserId();
        summaryId = UUID.randomUUID();
        summary = DailySummary.builder()
                .user(user)
                .targetDate(LocalDate.of(2026, 5, 6))
                .title("Spring Security")
                .content("# Spring Security\n\ncontent")
                .embedding(new float[]{0.1f})
                .build();
        githubAccount = new GithubAccount(user, "123", "octocat", "encrypted-token");
        repositoryConnection = new GithubRepositoryConnection(user, new ExternalGithubRepositoryResponse(
                100L,
                "til",
                "octocat/til",
                false,
                "main",
                "https://github.com/octocat/til"
        ));
    }

    @Test
    void requestCommit_validRequest_registersCommitRequestAndJob() {
        UUID jobId = UUID.randomUUID();
        mockValidSummaryAndGithub();
        when(filePolicy.createContentHash(summary.getContent())).thenReturn("content-hash");
        when(tilGithubCommitRepository.existsDuplicateContent(
                eq(repositoryConnection.getGithubRepositoryConnectionId()),
                eq("main"),
                eq("content-hash"),
                any()
        )).thenReturn(false);
        when(encryptor.decrypt("encrypted-token")).thenReturn("plain-token");
        when(filePathResolver.resolve(
                "plain-token",
                "octocat/til",
                "main",
                LocalDate.of(2026, 5, 6),
                "Spring Security"
        )).thenReturn("2026/05/06/spring-security.md");
        when(filePolicy.createCommitMessage("Spring Security")).thenReturn("docs: add TIL - Spring Security");
        when(tilGithubCommitRepository.save(any(TilGithubCommit.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(asyncJobManager.enqueue(eq(JobType.TIL_GITHUB_COMMIT), any(UUID.class))).thenReturn(jobId);

        TilGithubCommitService.RequestResult result = service.requestCommit(userId, summaryId);

        assertThat(result.jobId()).isEqualTo(jobId);
        assertThat(result.summaryId()).isEqualTo(summary.getSummaryId());
        assertThat(result.status()).isEqualTo(TilGithubCommitStatus.PENDING);

        ArgumentCaptor<TilGithubCommit> captor = ArgumentCaptor.forClass(TilGithubCommit.class);
        verify(tilGithubCommitRepository).save(captor.capture());
        TilGithubCommit saved = captor.getValue();
        assertThat(saved.getFilePath()).isEqualTo("2026/05/06/spring-security.md");
        assertThat(saved.getContentHash()).isEqualTo("content-hash");
        verify(tilAuditService).recordSuccess(
                eq(userId),
                eq(AuditEventType.TIL_COMMIT_REQUESTED),
                eq(AuditTargetType.TIL_GITHUB_COMMIT),
                eq(saved.getTilGithubCommitId()),
                any()
        );
    }

    @Test
    void requestCommit_withoutTitle_fails() {
        DailySummary titleEmptySummary = DailySummary.builder()
                .user(user)
                .targetDate(LocalDate.of(2026, 5, 6))
                .content("content")
                .embedding(new float[]{0.1f})
                .build();
        when(dailySummaryService.getSummary(summaryId)).thenReturn(titleEmptySummary);

        assertThatThrownBy(() -> service.requestCommit(userId, summaryId))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(TilErrorCode.TIL_TITLE_EMPTY);

        verify(tilGithubCommitRepository, never()).save(any());
    }

    @Test
    void requestCommit_withoutContent_fails() {
        DailySummary contentEmptySummary = DailySummary.builder()
                .user(user)
                .targetDate(LocalDate.of(2026, 5, 6))
                .title("Spring Security")
                .embedding(new float[]{0.1f})
                .build();
        when(dailySummaryService.getSummary(summaryId)).thenReturn(contentEmptySummary);

        assertThatThrownBy(() -> service.requestCommit(userId, summaryId))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(TilErrorCode.TIL_CONTENT_EMPTY);

        verify(tilGithubCommitRepository, never()).save(any());
    }

    @Test
    void requestCommit_withDuplicateContentCommit_fails() {
        mockValidSummaryAndGithub();
        when(filePolicy.createContentHash(summary.getContent())).thenReturn("content-hash");
        when(tilGithubCommitRepository.existsDuplicateContent(
                eq(repositoryConnection.getGithubRepositoryConnectionId()),
                eq("main"),
                eq("content-hash"),
                any()
        )).thenReturn(true);

        assertThatThrownBy(() -> service.requestCommit(userId, summaryId))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(TilErrorCode.TIL_ALREADY_COMMITTED);

        verify(tilGithubCommitRepository, never()).save(any());
        verify(tilAuditService).recordFailure(
                eq(userId),
                eq(AuditEventType.TIL_COMMIT_DUPLICATE_BLOCKED),
                eq(AuditTargetType.DAILY_SUMMARY),
                eq(summaryId),
                eq(TilErrorCode.TIL_ALREADY_COMMITTED),
                any()
        );
    }

    @Test
    void requestCommit_withoutConnectedRepository_fails() {
        when(dailySummaryService.getSummary(summaryId)).thenReturn(summary);
        when(githubAccountRepository.findByUser_UserId(userId)).thenReturn(Optional.of(githubAccount));
        when(githubRepositoryConnectionRepository.findByUser_UserId(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.requestCommit(userId, summaryId))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(TilErrorCode.TIL_GITHUB_REPOSITORY_NOT_CONNECTED);

        verify(tilGithubCommitRepository, never()).save(any());
    }

    private void mockValidSummaryAndGithub() {
        when(dailySummaryService.getSummary(summaryId)).thenReturn(summary);
        when(githubAccountRepository.findByUser_UserId(userId)).thenReturn(Optional.of(githubAccount));
        when(githubRepositoryConnectionRepository.findByUser_UserId(userId))
                .thenReturn(Optional.of(repositoryConnection));
    }
}
