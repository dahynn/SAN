package com.san.api.domain.til.service;

import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.TilErrorCode;
import com.san.api.global.external.github.client.GithubApiClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 같은 날짜 TIL 제목 중복 시 빈 GitHub 파일 경로를 찾는 정책 테스트 */
class TilGithubFilePathResolverTest {

    private final TilGithubFilePolicy filePolicy = new TilGithubFilePolicy();
    private final GithubApiClient githubApiClient = mock(GithubApiClient.class);
    private final TilGithubFilePathResolver resolver = new TilGithubFilePathResolver(filePolicy, githubApiClient);

    @Test
    @DisplayName("같은 날짜에 같은 제목 파일이 없으면 기본 경로를 반환한다")
    void resolveDefaultPath() {
        when(githubApiClient.existsContent(
                "token",
                "san",
                "til",
                "2026/05/06/spring-security.md",
                "main"
        )).thenReturn(false);

        String filePath = resolver.resolve(
                "token",
                "san/til",
                "main",
                LocalDate.of(2026, 5, 6),
                "Spring Security"
        );

        assertThat(filePath).isEqualTo("2026/05/06/spring-security.md");
    }

    @Test
    @DisplayName("같은 날짜에 같은 제목 파일이 있으면 suffix를 붙인 경로를 반환한다")
    void resolveSuffixedPath() {
        when(githubApiClient.existsContent(
                "token",
                "san",
                "til",
                "2026/05/06/spring-security.md",
                "main"
        )).thenReturn(true);
        when(githubApiClient.existsContent(
                "token",
                "san",
                "til",
                "2026/05/06/spring-security-1.md",
                "main"
        )).thenReturn(false);

        String filePath = resolver.resolve(
                "token",
                "san/til",
                "main",
                LocalDate.of(2026, 5, 6),
                "Spring Security"
        );

        assertThat(filePath).isEqualTo("2026/05/06/spring-security-1.md");
    }

    @Test
    @DisplayName("다른 날짜의 같은 제목 파일은 중복으로 보지 않는다")
    void resolveSameTitleOnDifferentDate() {
        when(githubApiClient.existsContent(
                "token",
                "san",
                "til",
                "2026/05/07/spring-security.md",
                "main"
        )).thenReturn(false);

        String filePath = resolver.resolve(
                "token",
                "san/til",
                "main",
                LocalDate.of(2026, 5, 7),
                "Spring Security"
        );

        assertThat(filePath).isEqualTo("2026/05/07/spring-security.md");
    }

    @Test
    @DisplayName("suffix 상한까지 같은 제목 파일이 존재하면 예외가 발생한다")
    void resolveUnavailablePath() {
        for (int suffix = 0; suffix <= 5; suffix++) {
            String fileName = suffix == 0 ? "spring-security.md" : "spring-security-" + suffix + ".md";
            when(githubApiClient.existsContent(
                    "token",
                    "san",
                    "til",
                    "2026/05/06/" + fileName,
                    "main"
            )).thenReturn(true);
        }

        assertThatThrownBy(() -> resolver.resolve(
                "token",
                "san/til",
                "main",
                LocalDate.of(2026, 5, 6),
                "Spring Security"
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(TilErrorCode.TIL_GITHUB_FILE_PATH_UNAVAILABLE);
    }

    @Test
    @DisplayName("owner/repo 형식이 아니면 예외가 발생한다")
    void invalidRepositoryFullName() {
        assertThatThrownBy(() -> resolver.resolve(
                "token",
                "invalid",
                "main",
                LocalDate.of(2026, 5, 6),
                "Spring Security"
        )).isInstanceOf(IllegalArgumentException.class);
    }
}
