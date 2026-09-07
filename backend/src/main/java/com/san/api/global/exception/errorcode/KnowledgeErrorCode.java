package com.san.api.global.exception.errorcode;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/** 지식카드 도메인 에러 코드 */
@Getter
@AllArgsConstructor
public enum KnowledgeErrorCode implements ErrorCode {

    CARD_NOT_FOUND(HttpStatus.NOT_FOUND, "K001", "존재하지 않는 지식 카드입니다."),
    CARD_ACCESS_DENIED(HttpStatus.FORBIDDEN, "K002", "해당 지식 카드에 대한 접근 권한이 없습니다."),
    CARD_ALREADY_EXISTS(HttpStatus.CONFLICT, "K003", "이미 지식카드가 생성된 수집 원본입니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
