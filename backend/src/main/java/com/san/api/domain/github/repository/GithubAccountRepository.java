package com.san.api.domain.github.repository;

import com.san.api.domain.github.entity.GithubAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/** GitHub 계정 연결 정보를 조회하는 JPA repository. */
public interface GithubAccountRepository extends JpaRepository<GithubAccount, UUID> {

    Optional<GithubAccount> findByUser_UserId(UUID userId);

    Optional<GithubAccount> findByGithubUserId(String githubUserId);

    void deleteByUser_UserId(UUID userId);
}
