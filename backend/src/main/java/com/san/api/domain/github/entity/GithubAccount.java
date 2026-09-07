package com.san.api.domain.github.entity;

import com.san.api.domain.user.entity.User;
import com.san.api.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** 서비스 사용자와 연결된 GitHub 계정 및 암호화된 access token 엔티티. */
@Entity
@Table(name = "github_accounts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GithubAccount extends BaseEntity {

    @Id
    @Column(name = "github_account_id", columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID githubAccountId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "github_user_id", nullable = false, unique = true)
    private String githubUserId;

    @Column(name = "github_username", nullable = false)
    private String githubUsername;

    @Column(name = "access_token_encrypted", nullable = false, length = 1000)
    private String accessTokenEncrypted;

    public GithubAccount(User user, String githubUserId, String githubUsername, String accessTokenEncrypted) {
        this.githubAccountId = UUID.randomUUID();
        this.user = user;
        this.githubUserId = githubUserId;
        this.githubUsername = githubUsername;
        this.accessTokenEncrypted = accessTokenEncrypted;
    }

    public void updateToken(String githubUsername, String accessTokenEncrypted) {
        this.githubUsername = githubUsername;
        this.accessTokenEncrypted = accessTokenEncrypted;
    }
}
