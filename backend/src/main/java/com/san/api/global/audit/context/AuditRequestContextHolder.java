package com.san.api.global.audit.context;

import java.util.Optional;

/**
 * 현재 요청 스레드의 감사 로그 컨텍스트를 보관하는 ThreadLocal holder.
 *
 * RequestLoggingFilter가 요청 시작 시 값을 넣고 요청 종료 시 반드시 제거한다.
 */
public final class AuditRequestContextHolder {

    private static final ThreadLocal<AuditRequestContext> CONTEXT = new ThreadLocal<>();

    private AuditRequestContextHolder() {
    }

    public static void set(AuditRequestContext context) {
        CONTEXT.set(context);
    }

    public static Optional<AuditRequestContext> get() {
        return Optional.ofNullable(CONTEXT.get());
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
