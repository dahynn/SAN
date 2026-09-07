package com.san.api.global.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.san.api.global.exception.errorcode.ErrorCode;
import com.san.api.global.response.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Spring Security 계층의 오류 응답을 프로젝트 공통 응답 형식으로 작성합니다.
 *
 * Security Handler들은 컨트롤러 계층 밖에서 실행되므로
 * {@code ResponseEntity}나 {@code GlobalExceptionHandler}를 거치지 않습니다.
 * 이 클래스가 {@code HttpServletResponse}에 직접 JSON을 작성해 응답 포맷을 일관되게 유지합니다.
 */
@Component
@RequiredArgsConstructor
public class SecurityErrorResponseWriter {

    private final ObjectMapper objectMapper;

    /**
     * 지정한 오류 코드로 {@code ApiResponse.error(...)} JSON 응답을 작성합니다.
     *
     * @param response JSON을 작성할 응답 객체
     * @param errorCode HTTP 상태와 오류 메시지를 가진 오류 코드
     * @throws IOException 응답 본문 작성 실패 시
     */
    public void write(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setStatus(errorCode.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), ApiResponse.error(errorCode, errorCode.getMessage()));
    }
}
