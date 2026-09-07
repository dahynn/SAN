package com.san.api.global.external.s3.dto.response;

/** S3 Presigned URL 발급 응답 DTO */
public record S3PresignedUrlResponse(
        String uploadUrl,
        String objectKey,
        long expiresInSeconds
) {
}
