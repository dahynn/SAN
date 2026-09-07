package com.san.api.global.external.s3.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.Set;

/** S3 연동 설정 값 */
@Validated
@ConfigurationProperties(prefix = "aws.s3")
public record S3Properties(
        @NotBlank String bucket,
        @NotBlank String region,
        @Positive long presignedUrlExpirationMinutes,
        @Positive long maxFileSizeBytes,
        @NotEmpty Set<@NotBlank String> allowedExtensions,
        @NotEmpty Set<@NotBlank String> allowedContentTypes
) {
}
