package com.san.api.global.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.san.api.global.audit.context.AuditRequestContextHolder;
import com.san.api.global.exception.errorcode.ErrorCode;

import java.time.Instant;

/**
 * 모든 API 응답의 공통 래퍼.
 * null 필드는 직렬화에서 제외됩니다.
 *
 * @param ok        성공 여부
 * @param data      성공 시 페이로드
 * @param error     실패 시 ErrorCode 이름
 * @param message   실패 시 오류 메시지
 * @param timestamp 응답 생성 시각 (UTC)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean ok,
        T data,
        String error,
        String message,
        String traceId,
        Instant timestamp) {

    /**
     * 데이터를 포함한 성공 응답
     *
     * @param data 응답 페이로드
     * @return ok=true, data 포함 응답
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null, null, null, Instant.now());
    }

    /**
     * 데이터 없는 성공 응답 (삭제 등 204 대용)
     *
     * @return ok=true, data=null 응답
     */
    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(true, null, null, null, null, Instant.now());
    }

    /**
     * 오류 응답
     *
     * @param errorCode 오류 코드
     * @param message   클라이언트에 전달할 오류 메시지
     * @return ok=false, error/message 포함 응답
     */
    public static <T> ApiResponse<T> error(ErrorCode errorCode, String message) {
        return new ApiResponse<>(false, null, errorCode.getCode(), message, currentTraceId(), Instant.now());
    }

    private static String currentTraceId() {
        return AuditRequestContextHolder.get()
                .map(context -> context.traceId())
                .orElse(null);
    }
}
