package com.san.api.global.exception.errorcode;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/** 스크랩 도메인 에러 코드 (S 계열) */
@Getter
@AllArgsConstructor
public enum ScrapErrorCode implements ErrorCode {

    EMPTY_SOURCE(HttpStatus.BAD_REQUEST, "S001", "저장할 수집 원본이 비어 있습니다."),
    SCRAP_ACCESS_DENIED(HttpStatus.FORBIDDEN, "S002", "해당 원본에 대한 접근 권한이 없습니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
