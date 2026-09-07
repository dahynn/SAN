package com.san.api.domain.scrap.service;

import com.san.api.domain.scrap.entity.SourceType;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

/** 수집 원본 값을 기준으로 SourceType을 감지하는 컴포넌트 */
@Component
public class SourceTypeDetector {

    /**
     * 원본 입력값 기준 SourceType 판별
     *
     * @param rawContent 사용자가 입력하거나 붙여넣은 원본 값
     * @return 감지된 수집 원본 유형
     */
    public SourceType detect(String rawContent) {
        if (isImageUrl(rawContent)) {
            return SourceType.IMAGE;
        }

        if (isHttpUrl(rawContent)) {
            return SourceType.LINK;
        }

        return SourceType.TEXT;
    }

    /**
     * 이미지 확장자 URL 여부
     *
     * @param value 검사할 원본 값
     * @return 이미지 URL 여부
     */
    private boolean isImageUrl(String value) {
        if (!isHttpUrl(value)) {
            return false;
        }

        String lowerValue = value.trim().toLowerCase(Locale.ROOT);
        return lowerValue.endsWith(".jpg")
                || lowerValue.endsWith(".jpeg")
                || lowerValue.endsWith(".png")
                || lowerValue.endsWith(".gif")
                || lowerValue.endsWith(".webp")
                || lowerValue.endsWith(".bmp")
                || lowerValue.endsWith(".svg");
    }

    /**
     * http/https URL 여부
     *
     * @param value 검사할 원본 값
     * @return URL 여부
     */
    private boolean isHttpUrl(String value) {
        if (isBlank(value)) {
            return false;
        }

        try {
            URI uri = new URI(value.trim());
            String scheme = uri.getScheme();
            return uri.getHost() != null && ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme));
        } catch (URISyntaxException e) {
            return false;
        }
    }

    /**
     * 빈 값 여부
     *
     * @param value 검사할 원본 값
     * @return null, 빈 문자열, 공백 문자열 여부
     */
    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
