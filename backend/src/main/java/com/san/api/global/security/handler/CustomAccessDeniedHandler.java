package com.san.api.global.security.handler;

import com.san.api.global.exception.errorcode.CommonErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 인증은 되었지만 권한이 부족한 요청을 공통 오류 응답으로 변환합니다.
 *
 * Spring Security의 인가 실패는 컨트롤러 전에 처리되므로, 이 Handler에서
 * 403 응답을 {@code ApiResponse} 형식으로 맞춥니다.
 */
@Component
@RequiredArgsConstructor
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final SecurityErrorResponseWriter responseWriter;

    /**
     * 인가 실패 응답을 작성합니다.
     *
     * @param request 인가 실패 요청
     * @param response 오류 응답을 작성할 응답 객체
     * @param accessDeniedException Spring Security가 전달한 인가 예외
     * @throws IOException 응답 본문 작성 실패 시
     */
    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        responseWriter.write(response, CommonErrorCode.FORBIDDEN);
    }
}
