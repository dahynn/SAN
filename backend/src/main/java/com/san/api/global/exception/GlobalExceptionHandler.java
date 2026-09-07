package com.san.api.global.exception;

import com.san.api.global.exception.errorcode.CommonErrorCode;
import com.san.api.global.exception.errorcode.ErrorCode;
import com.san.api.global.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 전역 예외 처리기.
 *
 * 컨트롤러 계층에서 발생하는 모든 예외를 잡아 {@link ApiResponse} 형식의
 * 일관된 오류 응답으로 변환합니다. 핸들러 우선순위는 구체적 예외 → 일반 예외 순입니다.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * BusinessException 처리.
     *
     * @param e 발생한 예외
     * @return ErrorCode에 정의된 HTTP 상태와 오류 응답
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();
        return ResponseEntity.status(errorCode.getStatus())
                .body(ApiResponse.error(errorCode, e.getMessage()));
    }

    /**
     * @Valid 검증 실패 처리. 첫 번째 필드 오류만 반환합니다.
     * BindException(@ModelAttribute)과 그 하위인 MethodArgumentNotValidException(@RequestBody)을 모두 처리합니다.
     *
     * @param e 발생한 예외
     * @return 400 응답
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<Void>> handleBindException(BindException e) {
        FieldError fieldError = e.getBindingResult().getFieldErrors().stream().findFirst().orElse(null);
        String message = fieldError == null
                ? CommonErrorCode.INVALID_INPUT_VALUE.getMessage()
                : fieldError.getField() + ": " + fieldError.getDefaultMessage();

        return ResponseEntity.status(CommonErrorCode.INVALID_INPUT_VALUE.getStatus())
                .body(ApiResponse.error(CommonErrorCode.INVALID_INPUT_VALUE, message));
    }

    /**
     * JSON 형식 오류, 누락된 요청 본문 등 읽을 수 없는 요청 처리.
     *
     * @param e 발생한 예외
     * @return 400 응답
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleMessageNotReadableException(HttpMessageNotReadableException e) {
        return ResponseEntity.status(CommonErrorCode.INVALID_INPUT_VALUE.getStatus())
                .body(ApiResponse.error(CommonErrorCode.INVALID_INPUT_VALUE,
                        CommonErrorCode.INVALID_INPUT_VALUE.getMessage()));
    }

    /**
     * 위에서 잡히지 않은 예외 처리 (fallback).
     * 스택 트레이스는 로그에만 남기고 클라이언트에는 노출하지 않습니다.
     *
     * @param e 처리되지 않은 예외
     * @return 500 응답
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("Unhandled exception", e);
        return ResponseEntity.status(CommonErrorCode.INTERNAL_SERVER_ERROR.getStatus())
                .body(ApiResponse.error(CommonErrorCode.INTERNAL_SERVER_ERROR,
                        CommonErrorCode.INTERNAL_SERVER_ERROR.getMessage()));
    }
}
