package com.san.api.domain.github.repository;

import com.san.api.domain.github.entity.GithubRepositoryConnection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/** 사용자별 GitHub 레포지토리 연결 정보를 조회하는 repository. */
public interface GithubRepositoryConnectionRepository extends JpaRepository<GithubRepositoryConnection, UUID> {

    Optional<GithubRepositoryConnection> findByUser_UserId(UUID userId);

    Optional<GithubRepositoryConnection> findByUser_UserIdAndGithubRepositoryId(UUID userId, Long githubRepositoryId);

    void deleteAllByUser_UserId(UUID userId);
}
