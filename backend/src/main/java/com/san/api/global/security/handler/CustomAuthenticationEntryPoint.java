package com.san.api.global.security.handler;

import com.san.api.global.exception.errorcode.CommonErrorCode;
import com.san.api.global.exception.errorcode.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 인증되지 않은 요청을 공통 오류 응답으로 변환합니다.
 *
 * Spring Security 필터 체인에서 인증 실패가 발생하면 컨트롤러까지 요청이
 * 도달하지 않으므로 {@code GlobalExceptionHandler}가 처리할 수 없습니다.
 * 이 EntryPoint는 Security 계층에서 발생한 401 응답도 {@code ApiResponse}
 * 형식으로 내려주기 위한 어댑터입니다.
 */
@Component
@RequiredArgsConstructor
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final SecurityErrorResponseWriter responseWriter;

    /**
     * 인증 실패 응답을 작성합니다.
     *
     * @param request 인증 실패 원인 정보가 담긴 요청 객체
     * @param response 오류 응답을 작성할 응답 객체
     * @param authException Spring Security가 전달한 인증 예외
     * @throws IOException 응답 본문 작성 실패 시
     */
    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        responseWriter.write(response, resolveErrorCode(request));
    }

    /**
     * JWT 필터가 request attribute에 남긴 구체적인 인증 실패 코드를 우선 사용합니다.
     *
     * 예를 들어 로그아웃된 토큰이면 {@code TOKEN_BLACKLISTED},
     * 잘못된 access token이면 {@code INVALID_ACCESS_TOKEN}을 사용합니다.
     * 필터가 남긴 코드가 없으면 일반적인 인증 실패로 처리합니다.</p>
     *
     * @param request 인증 실패 요청
     * @return 클라이언트에 내려줄 오류 코드
     */
    private ErrorCode resolveErrorCode(HttpServletRequest request) {
        Object errorCode = request.getAttribute(SecurityErrorAttribute.ERROR_CODE);
        if (errorCode instanceof ErrorCode code) {
            return code;
        }
        return CommonErrorCode.UNAUTHORIZED;
    }
}
