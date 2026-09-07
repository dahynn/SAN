package com.san.api.domain.til.service;

import com.san.api.domain.github.entity.GithubAccount;
import com.san.api.domain.github.entity.GithubRepositoryConnection;
import com.san.api.domain.github.repository.GithubAccountRepository;
import com.san.api.domain.github.repository.GithubRepositoryConnectionRepository;
import com.san.api.domain.til.entity.DailySummary;
import com.san.api.domain.til.entity.TilGithubCommit;
import com.san.api.domain.til.entity.TilGithubCommitStatus;
import com.san.api.domain.til.repository.TilGithubCommitRepository;
import com.san.api.global.async.entity.JobType;
import com.san.api.global.async.service.AsyncJobManager;
import com.san.api.global.audit.entity.AuditEventType;
import com.san.api.global.audit.entity.AuditTargetType;
import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.AuthErrorCode;
import com.san.api.global.exception.errorcode.TilErrorCode;
import com.san.api.global.security.crypto.AesGcmStringEncryptor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** TIL GitHub 커밋 요청을 검증하고 비동기 작업으로 등록하는 서비스 */
@Service
@RequiredArgsConstructor
public class TilGithubCommitService {

    private static final List<TilGithubCommitStatus> DUPLICATE_CHECK_STATUSES = List.of(
            TilGithubCommitStatus.PENDING,
            TilGithubCommitStatus.PROCESSING,
            TilGithubCommitStatus.COMPLETED
    );

    private final DailySummaryService dailySummaryService;
    private final GithubAccountRepository githubAccountRepository;
    private final GithubRepositoryConnectionRepository githubRepositoryConnectionRepository;
    private final TilGithubCommitRepository tilGithubCommitRepository;
    private final TilGithubFilePolicy filePolicy;
    private final TilGithubFilePathResolver filePathResolver;
    private final AsyncJobManager asyncJobManager;
    private final AesGcmStringEncryptor encryptor;
    private final TilAuditService tilAuditService;

    /**
     * TIL GitHub 커밋 요청을 등록합니다.
     *
     * @param userId 요청 사용자 ID
     * @param summaryId 커밋할 TIL ID
     * @return 등록된 커밋 요청과 비동기 작업 ID
     */
    public RequestResult requestCommit(UUID userId, UUID summaryId) {
        try {
            DailySummary summary = dailySummaryService.getSummary(summaryId);
            validateSummaryOwner(summary, userId);
            validateGeneratedTil(summary);

            GithubAccount githubAccount = githubAccountRepository.findByUser_UserId(userId)
                    .orElseThrow(() -> new BusinessException(AuthErrorCode.GITHUB_ACCOUNT_NOT_LINKED));
            GithubRepositoryConnection repositoryConnection = findSingleRepositoryConnection(userId);

            String branch = repositoryConnection.getDefaultBranch();
            String contentHash = filePolicy.createContentHash(summary.getContent());
            validateDuplicateContent(repositoryConnection, branch, contentHash);

            String accessToken = encryptor.decrypt(githubAccount.getAccessTokenEncrypted());
            String filePath = filePathResolver.resolve(
                    accessToken,
                    repositoryConnection.getFullName(),
                    branch,
                    summary.getTargetDate(),
                    summary.getTitle()
            );
            String commitMessage = filePolicy.createCommitMessage(summary.getTitle());

            TilGithubCommit commit = tilGithubCommitRepository.save(TilGithubCommit.builder()
                    .dailySummary(summary)
                    .githubRepositoryConnection(repositoryConnection)
                    .branch(branch)
                    .filePath(filePath)
                    .title(summary.getTitle().trim())
                    .contentHash(contentHash)
                    .commitMessage(commitMessage)
                    .build());

            UUID jobId = asyncJobManager.enqueue(JobType.TIL_GITHUB_COMMIT, commit.getTilGithubCommitId());
            tilAuditService.recordSuccess(
                    userId,
                    AuditEventType.TIL_COMMIT_REQUESTED,
                    AuditTargetType.TIL_GITHUB_COMMIT,
                    commit.getTilGithubCommitId(),
                    commitMetadata(commit, jobId)
            );
            return new RequestResult(
                    commit.getTilGithubCommitId(),
                    jobId,
                    summary.getSummaryId(),
                    commit.getStatus()
            );
        } catch (BusinessException e) {
            tilAuditService.recordFailure(
                    userId,
                    failureEventType(e),
                    AuditTargetType.DAILY_SUMMARY,
                    summaryId,
                    e.getErrorCode(),
                    Map.of("summaryId", summaryId)
            );
            throw e;
        }
    }

    private void validateSummaryOwner(DailySummary summary, UUID userId) {
        if (!summary.getUser().getUserId().equals(userId)) {
            throw new BusinessException(TilErrorCode.SUMMARY_ACCESS_DENIED);
        }
    }

    private void validateGeneratedTil(DailySummary summary) {
        if (isBlank(summary.getTitle())) {
            throw new BusinessException(TilErrorCode.TIL_TITLE_EMPTY);
        }
        if (isBlank(summary.getContent())) {
            throw new BusinessException(TilErrorCode.TIL_CONTENT_EMPTY);
        }
    }

    private void validateDuplicateContent(
            GithubRepositoryConnection repositoryConnection,
            String branch,
            String contentHash
    ) {
        boolean duplicated = tilGithubCommitRepository
                .existsDuplicateContent(
                        repositoryConnection.getGithubRepositoryConnectionId(),
                        branch,
                        contentHash,
                        DUPLICATE_CHECK_STATUSES
                );
        if (duplicated) {
            throw new BusinessException(TilErrorCode.TIL_ALREADY_COMMITTED);
        }
    }

    private GithubRepositoryConnection findSingleRepositoryConnection(UUID userId) {
        return githubRepositoryConnectionRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new BusinessException(TilErrorCode.TIL_GITHUB_REPOSITORY_NOT_CONNECTED));
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private AuditEventType failureEventType(BusinessException e) {
        if (e.getErrorCode() == TilErrorCode.TIL_ALREADY_COMMITTED) {
            return AuditEventType.TIL_COMMIT_DUPLICATE_BLOCKED;
        }
        return AuditEventType.TIL_COMMIT_FAILED;
    }

    private Map<String, Object> commitMetadata(TilGithubCommit commit, UUID jobId) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("jobId", jobId);
        metadata.put("summaryId", commit.getDailySummary().getSummaryId());
        metadata.put("githubRepositoryId", commit.getGithubRepositoryConnection().getGithubRepositoryId());
        metadata.put("repositoryFullName", commit.getGithubRepositoryConnection().getFullName());
        metadata.put("branch", commit.getBranch());
        metadata.put("filePath", commit.getFilePath());
        metadata.put("status", commit.getStatus().name());
        return metadata;
    }

    public record RequestResult(
            UUID commitId,
            UUID jobId,
            UUID summaryId,
            TilGithubCommitStatus status
    ) {
    }
}
