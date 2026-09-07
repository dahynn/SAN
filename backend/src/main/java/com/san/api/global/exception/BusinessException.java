package com.san.api.global.exception;

import com.san.api.global.exception.errorcode.ErrorCode;
import lombok.Getter;

/**
 * 서비스 비즈니스 규칙 위반 시 발생하는 런타임 예외.
 *
 * {@link ErrorCode}를 통해 HTTP 상태와 오류 분류를 함께 전달하며,
 * {@link GlobalExceptionHandler}에서 일괄 처리됩니다.
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    /**
     * ErrorCode에 정의된 기본 메시지로 예외를 생성합니다.
     *
     * @param errorCode 발생한 오류 코드
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    /**
     * 디버깅용 상세 메시지와 함께 예외를 생성합니다.
     * ex) 어떤 ID의 리소스가 없는지 구체적인 맥락을 담을 때 사용합니다.
     *
     * @param errorCode    발생한 오류 코드
     * @param debugMessage 응답 message 필드에 포함될 메시지
     */
    public BusinessException(ErrorCode errorCode, String debugMessage) {
        super(debugMessage);
        this.errorCode = errorCode;
    }
}
