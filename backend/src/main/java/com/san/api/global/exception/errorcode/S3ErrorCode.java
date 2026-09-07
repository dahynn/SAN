package com.san.api.global.exception.errorcode;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/** S3 업로드 에러 코드 */
@Getter
@AllArgsConstructor
public enum S3ErrorCode implements ErrorCode {

    INVALID_UPLOAD_FILE_NAME(HttpStatus.BAD_REQUEST, "S300", "업로드 파일명이 올바르지 않습니다."),
    UNSUPPORTED_UPLOAD_EXTENSION(HttpStatus.BAD_REQUEST, "S301", "지원하지 않는 파일 확장자입니다."),
    UNSUPPORTED_UPLOAD_CONTENT_TYPE(HttpStatus.BAD_REQUEST, "S302", "지원하지 않는 파일 형식입니다."),
    INVALID_UPLOAD_FILE_SIZE(HttpStatus.BAD_REQUEST, "S303", "업로드 파일 크기가 올바르지 않습니다."),
    UPLOAD_FILE_SIZE_EXCEEDED(HttpStatus.BAD_REQUEST, "S304", "업로드 가능한 파일 크기를 초과했습니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
