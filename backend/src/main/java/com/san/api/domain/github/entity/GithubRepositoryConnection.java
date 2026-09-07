package com.san.api.domain.github.entity;

import com.san.api.domain.user.entity.User;
import com.san.api.global.entity.BaseEntity;
import com.san.api.global.external.github.dto.response.ExternalGithubRepositoryResponse;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** 사용자가 서비스에 연결한 GitHub 레포지토리 정보를 저장하는 엔티티. */
@Entity
@Table(
        name = "github_repository_connections",
        indexes = {
                @Index(name = "idx_github_repo_connections_user_id", columnList = "user_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_github_repo_connections_user",
                        columnNames = "user_id"
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GithubRepositoryConnection extends BaseEntity {

    @Id
    @Column(name = "github_repository_connection_id", columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID githubRepositoryConnectionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "github_repository_id", nullable = false)
    private Long githubRepositoryId;

    @Column(nullable = false)
    private String name;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "default_branch", nullable = false)
    private String defaultBranch;

    @Column(name = "html_url", nullable = false)
    private String htmlUrl;

    @Column(name = "private_repository", nullable = false)
    private boolean privateRepository;

    public GithubRepositoryConnection(User user, ExternalGithubRepositoryResponse repository) {
        this.githubRepositoryConnectionId = UUID.randomUUID();
        this.user = user;
        this.githubRepositoryId = repository.id();
        update(repository);
    }

    public void update(ExternalGithubRepositoryResponse repository) {
        this.githubRepositoryId = repository.id();
        this.name = repository.name();
        this.fullName = repository.fullName();
        this.defaultBranch = repository.defaultBranch();
        this.htmlUrl = repository.htmlUrl();
        this.privateRepository = repository.privateRepository();
    }
}
