package com.san.api.domain.til.service;

import com.san.api.domain.github.entity.GithubAccount;
import com.san.api.domain.github.entity.GithubRepositoryConnection;
import com.san.api.domain.github.repository.GithubAccountRepository;
import com.san.api.domain.til.entity.DailySummary;
import com.san.api.domain.til.entity.TilGithubCommit;
import com.san.api.domain.til.repository.TilGithubCommitRepository;
import com.san.api.global.async.audit.AuditedAsyncJobRunner;
import com.san.api.global.async.entity.JobType;
import com.san.api.global.async.event.JobCreatedEvent;
import com.san.api.global.async.processor.AsyncJobProcessor;
import com.san.api.global.audit.entity.AuditEventType;
import com.san.api.global.audit.entity.AuditTargetType;
import com.san.api.global.audit.support.AuditFailureResolver;
import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.AuthErrorCode;
import com.san.api.global.exception.errorcode.TilErrorCode;
import com.san.api.global.external.github.client.GithubApiClient;
import com.san.api.global.external.github.dto.response.GithubCreateContentResponse;
import com.san.api.global.security.crypto.AesGcmStringEncryptor;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** TIL GitHub 커밋 비동기 작업 처리기 */
@Component
@RequiredArgsConstructor
public class TilGithubCommitJobProcessor implements AsyncJobProcessor {

    private static final String UNKNOWN_FAILURE_REASON_CODE = "TIL.UNKNOWN_FAILURE";
    private static final String FAILURE_MESSAGE_FALLBACK = "TIL GitHub 커밋 작업 처리 중 오류가 발생했습니다.";

    private final AuditedAsyncJobRunner auditedAsyncJobRunner;
    private final TilGithubCommitRepository tilGithubCommitRepository;
    private final GithubAccountRepository githubAccountRepository;
    private final GithubApiClient githubApiClient;
    private final AesGcmStringEncryptor encryptor;
    private final PlatformTransactionManager transactionManager;
    private final TilAuditService tilAuditService;
    private final AuditFailureResolver auditFailureResolver;

    @Override
    public JobType supports() {
        return JobType.TIL_GITHUB_COMMIT;
    }

    /**
     * TIL_GITHUB_COMMIT 작업 생성 이벤트를 수신합니다.
     *
     * @param event 비동기 작업 생성 이벤트
     */
    @Async("githubJobExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(JobCreatedEvent event) {
        handleIfSupported(event);
    }

    /**
     * TIL GitHub 커밋 작업을 감사 실행기로 위임해 처리합니다.
     *
     * @param jobId 비동기 작업 ID
     * @param targetId 커밋 요청 ID
     */
    @Override
    public void process(UUID jobId, UUID targetId) {
        auditedAsyncJobRunner.run(
                jobId,
                targetId,
                JobType.TIL_GITHUB_COMMIT,
                () -> processCommit(jobId, targetId),
                FAILURE_MESSAGE_FALLBACK
        );
    }

    private void processCommit(UUID jobId, UUID commitId) throws Exception {
        CommitPayload payload = null;

        try {
            markCommitProcessing(commitId);
            payload = loadPayload(commitId);
            GithubCreateContentResponse response = githubApiClient.createContent(
                    payload.accessToken(),
                    payload.owner(),
                    payload.repo(),
                    payload.filePath(),
                    payload.branch(),
                    payload.commitMessage(),
                    encodeContent(payload.content())
            );
            markCommitCompleted(commitId, response.commit().sha(), response.commit().htmlUrl(), LocalDateTime.now());
            recordCommitSucceeded(jobId, commitId, payload, response);
        } catch (Exception e) {
            String errorMessage = auditFailureResolver.failureMessage(e, FAILURE_MESSAGE_FALLBACK);
            markCommitFailedIfExists(commitId, errorMessage);
            recordCommitFailed(jobId, commitId, payload, e, errorMessage);
            throw e;
        }
    }

    private CommitPayload loadPayload(UUID commitId) {
        return transactionTemplate().execute(status -> {
            TilGithubCommit commit = getCommit(commitId);
            DailySummary summary = commit.getDailySummary();
            GithubRepositoryConnection repositoryConnection = commit.getGithubRepositoryConnection();
            GithubAccount githubAccount = githubAccountRepository.findByUser_UserId(summary.getUser().getUserId())
                    .orElseThrow(() -> new BusinessException(AuthErrorCode.GITHUB_ACCOUNT_NOT_LINKED));
            RepositoryName repositoryName = RepositoryName.from(repositoryConnection.getFullName());

            return new CommitPayload(
                    summary.getUser().getUserId(),
                    summary.getSummaryId(),
                    repositoryConnection.getGithubRepositoryId(),
                    repositoryConnection.getFullName(),
                    encryptor.decrypt(githubAccount.getAccessTokenEncrypted()),
                    repositoryName.owner(),
                    repositoryName.repo(),
                    commit.getFilePath(),
                    commit.getBranch(),
                    commit.getCommitMessage(),
                    summary.getContent()
            );
        });
    }

    private void markCommitProcessing(UUID commitId) {
        transactionTemplate().executeWithoutResult(status -> getCommit(commitId).markProcessing());
    }

    private void markCommitCompleted(UUID commitId, String commitSha, String commitUrl, LocalDateTime pushedAt) {
        transactionTemplate().executeWithoutResult(status -> {
            TilGithubCommit commit = getCommit(commitId);
            commit.markCompleted(commitSha, commitUrl, pushedAt);
        });
    }

    private void markCommitFailed(UUID commitId, String errorMessage) {
        transactionTemplate().executeWithoutResult(status -> getCommit(commitId).markFailed(errorMessage));
    }

    private void markCommitFailedIfExists(UUID commitId, String errorMessage) {
        try {
            markCommitFailed(commitId, errorMessage);
        } catch (BusinessException e) {
            if (e.getErrorCode() != TilErrorCode.TIL_GITHUB_COMMIT_NOT_FOUND) {
                throw e;
            }
        }
    }

    private void recordCommitSucceeded(
            UUID jobId,
            UUID commitId,
            CommitPayload payload,
            GithubCreateContentResponse response
    ) {
        tilAuditService.recordSuccess(
                payload.actorUserId(),
                AuditEventType.TIL_COMMIT_SUCCEEDED,
                AuditTargetType.TIL_GITHUB_COMMIT,
                commitId,
                commitMetadata(jobId, payload, response.commit().sha(), response.commit().htmlUrl())
        );
    }

    private void recordCommitFailed(
            UUID jobId,
            UUID commitId,
            CommitPayload payload,
            Exception exception,
            String errorMessage
    ) {
        CommitPayload auditPayload = payload == null ? loadAuditPayload(commitId).orElse(null) : payload;
        UUID actorUserId = auditPayload == null ? null : auditPayload.actorUserId();
        Map<String, Object> metadata = auditPayload == null
                ? fallbackFailureMetadata(jobId, commitId, exception)
                : failureMetadata(jobId, auditPayload, exception);
        tilAuditService.recordFailure(
                actorUserId,
                AuditEventType.TIL_COMMIT_FAILED,
                AuditTargetType.TIL_GITHUB_COMMIT,
                commitId,
                auditFailureResolver.failureReasonCode(exception, UNKNOWN_FAILURE_REASON_CODE),
                errorMessage,
                metadata
        );
    }

    private Optional<CommitPayload> loadAuditPayload(UUID commitId) {
        try {
            return Optional.ofNullable(transactionTemplate().execute(status -> {
                TilGithubCommit commit = getCommit(commitId);
                DailySummary summary = commit.getDailySummary();
                GithubRepositoryConnection repositoryConnection = commit.getGithubRepositoryConnection();
                RepositoryName repositoryName = RepositoryName.from(repositoryConnection.getFullName());
                return new CommitPayload(
                        summary.getUser().getUserId(),
                        summary.getSummaryId(),
                        repositoryConnection.getGithubRepositoryId(),
                        repositoryConnection.getFullName(),
                        null,
                        repositoryName.owner(),
                        repositoryName.repo(),
                        commit.getFilePath(),
                        commit.getBranch(),
                        commit.getCommitMessage(),
                        summary.getContent()
                );
            }));
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    private TilGithubCommit getCommit(UUID commitId) {
        return tilGithubCommitRepository.findByIdWithSummaryAndRepository(commitId)
                .orElseThrow(() -> new BusinessException(TilErrorCode.TIL_GITHUB_COMMIT_NOT_FOUND));
    }

    private Map<String, Object> commitMetadata(
            UUID jobId,
            CommitPayload payload,
            String commitSha,
            String commitUrl
    ) {
        Map<String, Object> metadata = baseCommitMetadata(jobId, payload);
        metadata.put("commitSha", commitSha);
        metadata.put("commitUrl", commitUrl);
        return metadata;
    }

    private Map<String, Object> failureMetadata(UUID jobId, CommitPayload payload, Exception exception) {
        Map<String, Object> metadata = baseCommitMetadata(jobId, payload);
        metadata.put("exceptionType", exception.getClass().getSimpleName());
        metadata.putAll(auditFailureResolver.failureMetadata(exception));
        return metadata;
    }

    private Map<String, Object> fallbackFailureMetadata(UUID jobId, UUID commitId, Exception exception) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("jobId", jobId);
        metadata.put("commitId", commitId);
        metadata.put("exceptionType", exception.getClass().getSimpleName());
        metadata.putAll(auditFailureResolver.failureMetadata(exception));
        return metadata;
    }

    private Map<String, Object> baseCommitMetadata(UUID jobId, CommitPayload payload) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("jobId", jobId);
        metadata.put("summaryId", payload.summaryId());
        metadata.put("githubRepositoryId", payload.githubRepositoryId());
        metadata.put("repositoryFullName", payload.repositoryFullName());
        metadata.put("branch", payload.branch());
        metadata.put("filePath", payload.filePath());
        return metadata;
    }

    private String encodeContent(String content) {
        return Base64.getEncoder().encodeToString(content.getBytes(StandardCharsets.UTF_8));
    }

    private TransactionTemplate transactionTemplate() {
        return new TransactionTemplate(transactionManager);
    }

    private record CommitPayload(
            UUID actorUserId,
            UUID summaryId,
            Long githubRepositoryId,
            String repositoryFullName,
            String accessToken,
            String owner,
            String repo,
            String filePath,
            String branch,
            String commitMessage,
            String content
    ) {
    }

    private record RepositoryName(String owner, String repo) {

        private static RepositoryName from(String fullName) {
            if (fullName == null || fullName.isBlank()) {
                throw new IllegalArgumentException("GitHub 저장소 전체 이름은 필수입니다.");
            }

            String[] parts = fullName.split("/", 2);
            if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
                throw new IllegalArgumentException("GitHub 저장소 전체 이름은 owner/repo 형식이어야 합니다.");
            }
            return new RepositoryName(parts[0], parts[1]);
        }
    }
}
