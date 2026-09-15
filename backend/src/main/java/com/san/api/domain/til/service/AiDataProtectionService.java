package com.san.api.domain.til.service;

import com.san.api.domain.til.entity.TilSourceSnapshot;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Pattern;

/** 외부 AI 전달 직전의 최소 개인정보 마스킹 정책입니다. 규제 준수 판정 기능은 아닙니다. */
@Service
public class AiDataProtectionService {

    private static final Pattern EMAIL = Pattern.compile("(?i)\\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}\\b");
    private static final Pattern PHONE = Pattern.compile("(?<!\\d)01[016789]-?\\d{3,4}-?\\d{4}(?!\\d)");
    private static final Pattern RESIDENT_REGISTRATION = Pattern.compile("(?<!\\d)\\d{6}-?[1-4]\\d{6}(?!\\d)");

    public ProtectionResult protect(List<TilSourceSnapshot> sources) {
        int maskedItemCount = 0;
        for (TilSourceSnapshot source : sources) {
            String original = source.getAiInputContent();
            String protectedContent = mask(original);
            if (!protectedContent.equals(original)) {
                maskedItemCount++;
                source.replaceAiInputContent(protectedContent);
            }
        }
        return new ProtectionResult(maskedItemCount);
    }

    private String mask(String value) {
        String masked = RESIDENT_REGISTRATION.matcher(value).replaceAll("[주민등록번호 마스킹]");
        masked = PHONE.matcher(masked).replaceAll("[휴대전화번호 마스킹]");
        return EMAIL.matcher(masked).replaceAll("[이메일 마스킹]");
    }

    public record ProtectionResult(int maskedItemCount) {
        public boolean requiresConfirmation() {
            return maskedItemCount > 0;
        }
    }
}
