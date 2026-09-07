package com.san.api.domain.til.entity;

import com.san.api.domain.github.entity.GithubRepositoryConnection;
import com.san.api.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/** TIL을 GitHub 저장소에 커밋한 이력을 저장하는 엔티티 */
@Entity
@Table(
        name = "til_github_commits",
        indexes = {
                @Index(name = "idx_til_github_commits_summary_id", columnList = "summary_id"),
                @Index(name = "idx_til_github_commits_repository_branch_hash", columnList = "github_repository_connection_id, branch, content_hash")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TilGithubCommit extends BaseEntity {

    @Id
    @Column(name = "til_github_commit_id", columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID tilGithubCommitId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "summary_id", nullable = false)
    private DailySummary dailySummary;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "github_repository_connection_id", nullable = false)
    private GithubRepositoryConnection githubRepositoryConnection;

    @Column(nullable = false)
    private String branch;

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @Column(nullable = false)
    private String title;

    @Column(name = "content_hash", nullable = false, length = 64)
    private String contentHash;

    @Column(name = "commit_message", nullable = false, length = 300)
    private String commitMessage;

    @Column(name = "commit_sha")
    private String commitSha;

    @Column(name = "commit_url", length = 500)
    private String commitUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TilGithubCommitStatus status;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "pushed_at")
    private LocalDateTime pushedAt;

    @Builder
    public TilGithubCommit(
            DailySummary dailySummary,
            GithubRepositoryConnection githubRepositoryConnection,
            String branch,
            String filePath,
            String title,
            String contentHash,
            String commitMessage
    ) {
        this.tilGithubCommitId = UUID.randomUUID();
        this.dailySummary = dailySummary;
        this.githubRepositoryConnection = githubRepositoryConnection;
        this.branch = branch;
        this.filePath = filePath;
        this.title = title;
        this.contentHash = contentHash;
        this.commitMessage = commitMessage;
        this.status = TilGithubCommitStatus.PENDING;
    }

    /** GitHub 커밋 처리를 시작 상태로 변경합니다. */
    public void markProcessing() {
        this.status = TilGithubCommitStatus.PROCESSING;
        this.errorMessage = null;
    }

    /**
     * GitHub 커밋 성공 결과를 저장합니다.
     *
     * @param commitSha GitHub 커밋 SHA
     * @param commitUrl GitHub 커밋 URL
     * @param pushedAt GitHub 커밋 완료 시각
     */
    public void markCompleted(String commitSha, String commitUrl, LocalDateTime pushedAt) {
        this.status = TilGithubCommitStatus.COMPLETED;
        this.commitSha = commitSha;
        this.commitUrl = commitUrl;
        this.pushedAt = pushedAt;
        this.errorMessage = null;
    }

    /**
     * GitHub 커밋 실패 사유를 저장합니다.
     *
     * @param errorMessage 실패 사유
     */
    public void markFailed(String errorMessage) {
        this.status = TilGithubCommitStatus.FAILED;
        this.errorMessage = errorMessage;
    }
}
