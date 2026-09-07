package com.san.api.domain.til.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/** TIL GitHub 파일명/커밋 메시지/contentHash 정책이 바뀌지 않도록 검증하는 테스트 */
class TilGithubFilePolicyTest {

    private final TilGithubFilePolicy policy = new TilGithubFilePolicy();

    @Test
    @DisplayName("TIL 제목과 날짜로 GitHub 파일 경로를 생성한다")
    void createFilePath() {
        String filePath = policy.createFilePath(
                LocalDate.of(2026, 5, 6),
                "Spring Security Filter Chain"
        );

        assertThat(filePath).isEqualTo("2026/05/06/spring-security-filter-chain.md");
    }

    @Test
    @DisplayName("파일명에 사용할 수 없는 문자는 하이픈으로 변환한다")
    void createSlug() {
        String slug = policy.createSlug("  JPA: Transaction 전파 옵션 정리!  ");

        assertThat(slug).isEqualTo("jpa-transaction-전파-옵션-정리");
    }

    @Test
    @DisplayName("제목이 비어 있으면 기본 slug를 사용한다")
    void createDefaultSlug() {
        String slug = policy.createSlug("   ");

        assertThat(slug).isEqualTo("til");
    }

    @Test
    @DisplayName("TIL 제목으로 커밋 메시지를 생성한다")
    void createCommitMessage() {
        String commitMessage = policy.createCommitMessage("Spring Security");

        assertThat(commitMessage).isEqualTo("docs: add TIL - Spring Security");
    }

    @Test
    @DisplayName("본문 해시는 줄바꿈 차이와 양끝 공백을 무시한다")
    void createContentHash() {
        String hash = policy.createContentHash(" hello\r\nworld \n");
        String sameHash = policy.createContentHash("hello\nworld");

        assertThat(hash).isEqualTo(sameHash);
    }
}
