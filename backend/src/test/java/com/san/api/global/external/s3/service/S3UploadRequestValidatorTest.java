package com.san.api.global.external.s3.service;

import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.S3ErrorCode;
import com.san.api.global.external.s3.config.S3Properties;
import com.san.api.global.external.s3.dto.request.S3PresignedUrlRequest;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

class S3UploadRequestValidatorTest {

    private static final long MAX_FILE_SIZE_BYTES = 10_485_760L;

    private final S3UploadRequestValidator validator = new S3UploadRequestValidator(
            new S3Properties(
                    "san-bucket",
                    "us-east-1",
                    5,
                    MAX_FILE_SIZE_BYTES,
                    Set.of("jpg", "jpeg", "png", "webp"),
                    Set.of("image/jpeg", "image/png", "image/webp")
            )
    );

    @Test
    void validImageUploadRequestPasses() {
        S3PresignedUrlRequest request = new S3PresignedUrlRequest(
                "capture.WEBP",
                "IMAGE/WEBP",
                MAX_FILE_SIZE_BYTES
        );

        assertThatCode(() -> validator.validate(request))
                .doesNotThrowAnyException();
    }

    @Test
    void validImageUploadRequestWithOuterSpacesPassesAfterTrim() {
        S3PresignedUrlRequest request = new S3PresignedUrlRequest(
                " capture.WEBP ",
                " image/webp ",
                MAX_FILE_SIZE_BYTES
        );

        assertThatCode(() -> validator.validate(request))
                .doesNotThrowAnyException();
    }

    @Test
    void invalidFileNameThrowsException() {
        S3PresignedUrlRequest request = new S3PresignedUrlRequest(
                "images/capture.png",
                "image/png",
                1024L
        );

        assertS3ErrorCode(request, S3ErrorCode.INVALID_UPLOAD_FILE_NAME);
    }

    @Test
    void unsupportedExtensionThrowsException() {
        S3PresignedUrlRequest request = new S3PresignedUrlRequest(
                "capture.gif",
                "image/png",
                1024L
        );

        assertS3ErrorCode(request, S3ErrorCode.UNSUPPORTED_UPLOAD_EXTENSION);
    }

    @Test
    void unsupportedContentTypeThrowsException() {
        S3PresignedUrlRequest request = new S3PresignedUrlRequest(
                "capture.png",
                "image/gif",
                1024L
        );

        assertS3ErrorCode(request, S3ErrorCode.UNSUPPORTED_UPLOAD_CONTENT_TYPE);
    }

    @Test
    void invalidFileSizeThrowsException() {
        S3PresignedUrlRequest request = new S3PresignedUrlRequest(
                "capture.png",
                "image/png",
                0L
        );

        assertS3ErrorCode(request, S3ErrorCode.INVALID_UPLOAD_FILE_SIZE);
    }

    @Test
    void exceededFileSizeThrowsException() {
        S3PresignedUrlRequest request = new S3PresignedUrlRequest(
                "capture.png",
                "image/png",
                MAX_FILE_SIZE_BYTES + 1
        );

        assertS3ErrorCode(request, S3ErrorCode.UPLOAD_FILE_SIZE_EXCEEDED);
    }

    private void assertS3ErrorCode(S3PresignedUrlRequest request, S3ErrorCode errorCode) {
        assertThatThrownBy(() -> validator.validate(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(errorCode);
    }
}
