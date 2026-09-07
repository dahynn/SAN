package com.san.api.domain.til.service;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.Locale;
import java.util.regex.Pattern;

/** TIL GitHub 커밋 파일명, 경로, 해시 정책 */
@Component
public class TilGithubFilePolicy {

    private static final Pattern NON_SLUG_CHARACTER = Pattern.compile("[^\\p{IsAlphabetic}\\p{IsDigit}]+");
    private static final Pattern SLUG_EDGE_HYPHEN = Pattern.compile("(^-+|-+$)");
    private static final String DEFAULT_SLUG = "til";

    /**
     * TIL 제목과 날짜를 기반으로 GitHub 파일 경로를 생성합니다.
     *
     * @param targetDate TIL 대상 날짜
     * @param title TIL 제목
     * @return GitHub 저장소에 생성할 TIL 파일 경로
     */
    public String createFilePath(LocalDate targetDate, String title) {
        return createDirectoryPath(targetDate) + "/" + createSlug(title) + ".md";
    }

    /**
     * TIL 날짜를 기반으로 날짜별 디렉터리 경로를 생성합니다.
     *
     * @param targetDate TIL 대상 날짜
     * @return 날짜별 디렉터리 경로
     */
    public String createDirectoryPath(LocalDate targetDate) {
        return "%04d/%02d/%02d".formatted(
                targetDate.getYear(),
                targetDate.getMonthValue(),
                targetDate.getDayOfMonth()
        );
    }

    /**
     * TIL 제목을 GitHub 파일명에 사용할 slug로 변환합니다.
     *
     * @param title TIL 제목
     * @return 파일명에 사용할 slug
     */
    public String createSlug(String title) {
        String normalized = title == null ? "" : title.trim().toLowerCase(Locale.ROOT);
        String slug = NON_SLUG_CHARACTER.matcher(normalized).replaceAll("-");
        slug = SLUG_EDGE_HYPHEN.matcher(slug).replaceAll("");

        if (slug.isBlank()) {
            return DEFAULT_SLUG;
        }
        return slug;
    }

    /**
     * TIL 제목을 기반으로 GitHub 커밋 메시지를 생성합니다.
     *
     * @param title TIL 제목
     * @return GitHub 커밋 메시지
     */
    public String createCommitMessage(String title) {
        return "docs: add TIL - " + title.trim();
    }

    /**
     * TIL 본문을 정규화한 뒤 SHA-256 해시를 생성합니다.
     *
     * @param content TIL 마크다운 본문
     * @return SHA-256 content hash
     */
    public String createContentHash(String content) {
        String normalized = normalizeContent(content);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(normalized.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm is not available.", e);
        }
    }

    /**
     * TIL 본문 중복 체크를 위해 줄바꿈과 양끝 공백을 정규화합니다.
     *
     * @param content TIL 마크다운 본문
     * @return 정규화된 TIL 마크다운 본문
     */
    public String normalizeContent(String content) {
        if (content == null) {
            return "";
        }
        return content.replace("\r\n", "\n")
                .replace("\r", "\n")
                .trim();
    }
}
