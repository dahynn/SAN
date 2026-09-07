package com.san.api.global.exception.errorcode;

import org.springframework.http.HttpStatus;

/**
 * 비즈니스 오류 코드 인터페이스.
 * 각 도메인별 enum이 이 인터페이스를 구현합니다.
 */
public interface ErrorCode {

    HttpStatus getStatus();

    /** 응답 error 필드에 노출되는 코드 문자열 (ex. "A001", "U001") */
    String getCode();

    String getMessage();
}
