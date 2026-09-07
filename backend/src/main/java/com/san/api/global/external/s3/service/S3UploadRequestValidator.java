package com.san.api.global.external.s3.service;

import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.S3ErrorCode;
import com.san.api.global.external.s3.config.S3Properties;
import com.san.api.global.external.s3.dto.request.S3PresignedUrlRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Set;

/** S3 업로드 요청 검증 컴포넌트 */
@Component
@RequiredArgsConstructor
public class S3UploadRequestValidator {

    private final S3Properties s3Properties;

    /**
     * Presigned URL 발급 전 업로드 요청 검증
     *
     * @param request Presigned URL 발급 요청
     */
    public void validate(S3PresignedUrlRequest request) {
        validateFileName(request.fileName());
        validateExtension(request.fileName());
        validateContentType(request.contentType());
        validateFileSize(request.fileSize());
    }

    /** 파일명 공백 및 경로 문자 포함 여부 검증 */
    private void validateFileName(String fileName) {
        if (!StringUtils.hasText(fileName) || fileName.contains("/") || fileName.contains("\\")) {
            throw new BusinessException(S3ErrorCode.INVALID_UPLOAD_FILE_NAME);
        }
    }

    /** 파일 확장자 존재 및 허용 여부 검증 */
    private void validateExtension(String fileName) {
        String extension = extractExtension(fileName);
        if (!StringUtils.hasText(extension) || !isAllowedExtension(extension)) {
            throw new BusinessException(S3ErrorCode.UNSUPPORTED_UPLOAD_EXTENSION);
        }
    }

    /** 파일 Content-Type 존재 및 허용 여부 검증 */
    private void validateContentType(String contentType) {
        if (!StringUtils.hasText(contentType)
                || !containsIgnoreCase(s3Properties.allowedContentTypes(), contentType)) {
            throw new BusinessException(S3ErrorCode.UNSUPPORTED_UPLOAD_CONTENT_TYPE);
        }
    }

    /** 파일 크기 유효성 및 최대 크기 초과 여부 검증 */
    private void validateFileSize(Long fileSize) {
        if (fileSize == null || fileSize <= 0) {
            throw new BusinessException(S3ErrorCode.INVALID_UPLOAD_FILE_SIZE);
        }

        if (fileSize > s3Properties.maxFileSizeBytes()) {
            throw new BusinessException(S3ErrorCode.UPLOAD_FILE_SIZE_EXCEEDED);
        }
    }

    /** 허용 확장자 포함 여부 */
    private boolean isAllowedExtension(String extension) {
        return containsIgnoreCase(s3Properties.allowedExtensions(), extension);
    }

    /** 대소문자를 무시한 허용 값 포함 여부 */
    private boolean containsIgnoreCase(Set<String> allowedValues, String value) {
        String normalizedValue = value.trim().toLowerCase(Locale.ROOT);
        return allowedValues.stream()
                .map(allowedValue -> allowedValue.trim().toLowerCase(Locale.ROOT))
                .anyMatch(normalizedValue::equals);
    }

    /** 파일명에서 확장자 추출 */
    private String extractExtension(String fileName) {
        int extensionIndex = fileName.lastIndexOf('.');
        if (extensionIndex <= 0 || extensionIndex == fileName.length() - 1) {
            return "";
        }

        return fileName.substring(extensionIndex + 1);
    }
}
