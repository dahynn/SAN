package com.san.api.domain.til.service;

import com.san.api.domain.scrap.entity.SourceType;
import com.san.api.domain.til.entity.TilSourceSnapshot;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AiDataProtectionServiceTest {

    private final AiDataProtectionService service = new AiDataProtectionService();

    @Test
    void protect_masksEmailPhoneAndResidentRegistrationBeforeAiTransmission() {
        TilSourceSnapshot source = snapshot("문의: user@example.com, 010-1234-5678, 900101-1234567");

        AiDataProtectionService.ProtectionResult result = service.protect(List.of(source));

        assertThat(result.maskedItemCount()).isEqualTo(1);
        assertThat(result.requiresConfirmation()).isTrue();
        assertThat(source.getAiInputContent())
                .contains("[이메일 마스킹]", "[휴대전화번호 마스킹]", "[주민등록번호 마스킹]")
                .doesNotContain("user@example.com", "010-1234-5678", "900101-1234567");
    }

    @Test
    void protect_leavesOrdinaryStudyContentUntouched() {
        TilSourceSnapshot source = snapshot("Spring 트랜잭션 전파 옵션을 학습했다.");

        AiDataProtectionService.ProtectionResult result = service.protect(List.of(source));

        assertThat(result.maskedItemCount()).isZero();
        assertThat(result.requiresConfirmation()).isFalse();
        assertThat(source.getAiInputContent()).isEqualTo("Spring 트랜잭션 전파 옵션을 학습했다.");
    }

    private TilSourceSnapshot snapshot(String content) {
        return new TilSourceSnapshot(
                UUID.randomUUID(), UUID.randomUUID(), "테스트", SourceType.TEXT, content, null, null,
                UUID.randomUUID(), "테스트", LocalDateTime.now(), "text", content
        );
    }
}
