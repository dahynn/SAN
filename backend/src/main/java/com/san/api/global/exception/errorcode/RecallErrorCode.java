package com.san.api.global.exception.errorcode;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/** 리콜 에러 코드 */
@Getter
@AllArgsConstructor
public enum RecallErrorCode implements ErrorCode {

    RECALL_TIL_NOT_FOUND(HttpStatus.NOT_FOUND, "R001", "리콜 퀴즈를 생성할 TIL이 없습니다."),
    EMPTY_RECALL_SOURCE(HttpStatus.BAD_REQUEST, "R002", "리콜 퀴즈를 생성할 원본이 없습니다."),
    INVALID_RECALL_SOURCE_CONTENT(HttpStatus.BAD_REQUEST, "R003", "리콜 퀴즈를 생성할 원본 내용이 올바르지 않습니다."),
    RECALL_QUIZ_NOT_FOUND(HttpStatus.NOT_FOUND, "R004", "리콜 퀴즈를 찾을 수 없습니다."),
    INVALID_RECALL_QUIZ_ANSWER(HttpStatus.BAD_REQUEST, "R005", "리콜 퀴즈 답변이 올바르지 않습니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
