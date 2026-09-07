package com.san.api.global.logging;

import com.san.api.global.audit.context.AuditRequestContext;
import com.san.api.global.audit.context.AuditRequestContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 모든 HTTP 요청에 대해 메서드, URI, 처리 시간, 응답 상태를 INFO 레벨로 기록하는 필터.
 *
 * {@link OncePerRequestFilter}를 상속하여 요청 당 정확히 한 번만 실행됩니다.
 * 로그 형식: {@code [STATUS] METHOD URI - Xms}
 */
@Slf4j
@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final String TRACE_ID_HEADER = "X-Request-Id";
    private static final String FORWARDED_FOR_HEADER = "X-Forwarded-For";
    private static final String USER_AGENT_HEADER = "User-Agent";
    private static final int TRACE_ID_MAX_LENGTH = 100;
    private static final int IP_ADDRESS_MAX_LENGTH = 45;
    private static final Pattern TRACE_ID_PATTERN = Pattern.compile("^[A-Za-z0-9._~-]{1,100}$");
    private static final Pattern IPV4_PATTERN = Pattern.compile(
            "^(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)"
                    + "(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}$"
    );

    /**
     * 요청을 다음 필터 체인으로 전달하고, 완료 후 소요 시간과 응답 상태를 로깅합니다.
     * 예외 발생 시에도 {@code finally} 블록에서 반드시 로그를 기록합니다.
     *
     * @param request     현재 HTTP 요청
     * @param response    현재 HTTP 응답
     * @param filterChain 다음 필터 체인
     * @throws ServletException 서블릿 처리 중 오류 발생 시
     * @throws IOException      I/O 오류 발생 시
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        long start = System.currentTimeMillis();
        String traceId = resolveTraceId(request);
        response.setHeader(TRACE_ID_HEADER, traceId);
        AuditRequestContextHolder.set(new AuditRequestContext(
                traceId,
                resolveIpAddress(request),
                request.getHeader(USER_AGENT_HEADER)
        ));
        try {
            filterChain.doFilter(request, response);
        } finally {
            long elapsed = System.currentTimeMillis() - start;
            log.info("[{}] {} {} - {}ms",
                    response.getStatus(),
                    request.getMethod(),
                    request.getRequestURI(),
                    elapsed);
            AuditRequestContextHolder.clear();
        }
    }

    private String resolveTraceId(HttpServletRequest request) {
        String traceId = request.getHeader(TRACE_ID_HEADER);
        // 외부에서 전달된 trace id가 DB 컬럼 길이와 허용 형식을 벗어나면 안전한 UUID로 대체한다.
        if (!isValidTraceId(traceId)) {
            return UUID.randomUUID().toString();
        }
        return traceId;
    }

    private String resolveIpAddress(HttpServletRequest request) {
        String forwardedFor = request.getHeader(FORWARDED_FOR_HEADER);
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            // X-Forwarded-For는 여러 IP가 들어올 수 있으므로 최초 클라이언트 IP 후보만 검증한다.
            String clientIp = forwardedFor.split(",")[0].trim();
            if (isValidIpAddress(clientIp)) {
                return clientIp;
            }
        }
        // 헤더 값이 유효하지 않으면 서블릿 컨테이너가 제공하는 접속 주소를 감사 로그에 남긴다.
        return sanitizeRemoteAddr(request.getRemoteAddr());
    }

    private boolean isValidTraceId(String traceId) {
        // 감사 로그 trace_id 컬럼에 안전하게 저장할 수 있는 길이와 문자만 허용한다.
        return traceId != null
                && !traceId.isBlank()
                && traceId.length() <= TRACE_ID_MAX_LENGTH
                && TRACE_ID_PATTERN.matcher(traceId).matches();
    }

    private boolean isValidIpAddress(String ipAddress) {
        // IPv4와 IPv6 모두 DB 컬럼 길이 안에 들어가는 정상 IP 형식만 허용한다.
        if (ipAddress == null || ipAddress.isBlank() || ipAddress.length() > IP_ADDRESS_MAX_LENGTH) {
            return false;
        }
        return IPV4_PATTERN.matcher(ipAddress).matches() || isValidIpv6Address(ipAddress);
    }

    private boolean isValidIpv6Address(String ipAddress) {
        // ':'가 없는 값은 IPv6 후보가 아니므로 DNS 조회 없이 빠르게 제외한다.
        if (!ipAddress.contains(":")) {
            return false;
        }
        try {
            // JDK 파서를 사용해 축약 표기(::)를 포함한 IPv6 주소를 검증한다.
            return InetAddress.getByName(ipAddress) instanceof Inet6Address;
        } catch (Exception e) {
            return false;
        }
    }

    private String sanitizeRemoteAddr(String remoteAddr) {
        // remoteAddr도 컨테이너 제공 값이지만, DB 컬럼을 넘지 않도록 마지막으로 길이를 방어한다.
        if (remoteAddr == null || remoteAddr.isBlank() || remoteAddr.length() > IP_ADDRESS_MAX_LENGTH) {
            return null;
        }
        return remoteAddr;
    }
}
