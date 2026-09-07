package com.san.api.global.external.s3.dto.request;

/** S3 Presigned URL 발급 요청 DTO */
public record S3PresignedUrlRequest(
        String fileName,
        String contentType,
        Long fileSize
) {
    public S3PresignedUrlRequest {
        fileName = trim(fileName);
        contentType = trim(contentType);
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }
}
