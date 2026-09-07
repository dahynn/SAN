package com.san.api.domain.scrap.service;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** 스크랩 원본 중복 식별 해시 정책 */
@Component
public class ScrapContentHashPolicy {

    /**
     * 스크랩 원본을 정규화한 뒤 SHA-256 해시를 생성
     *
     * @param rawContent 사용자가 입력하거나 붙여넣은 원본 값
     * @return SHA-256 content hash
     */
    public String createContentHash(String rawContent) {
        String normalized = normalize(rawContent);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(normalized.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm is not available.", e);
        }
    }

    /**
     * 원본 중복 체크를 위해 줄바꿈과 양끝 공백을 정규화
     *
     * @param rawContent 사용자가 입력하거나 붙여넣은 원본 값
     * @return 정규화된 원본 값
     */
    public String normalize(String rawContent) {
        if (rawContent == null) {
            return "";
        }
        return rawContent.replace("\r\n", "\n")
                .replace("\r", "\n")
                .trim();
    }
}
