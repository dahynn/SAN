package com.san.api.domain.scrap.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** 스크랩 원본 중복 식별 해시 정책 테스트 */
class ScrapContentHashPolicyTest {

    private final ScrapContentHashPolicy policy = new ScrapContentHashPolicy();

    @Test
    @DisplayName("원본 해시는 줄바꿈 차이와 양끝 공백을 무시한다")
    void createContentHash() {
        String hash = policy.createContentHash(" hello\r\nworld \n");
        String sameHash = policy.createContentHash("hello\nworld");

        assertThat(hash).isEqualTo(sameHash);
    }

    @Test
    @DisplayName("원본 정규화는 CRLF와 CR 줄바꿈을 LF로 통일한다")
    void normalize() {
        String normalized = policy.normalize(" hello\r\nworld\r ");

        assertThat(normalized).isEqualTo("hello\nworld");
    }
}
