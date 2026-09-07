package com.san.api.global.audit.support;

import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class AuditFailureResolver {

    public String failureMessage(Throwable exception, String fallback) {
        StringBuilder sb = new StringBuilder();
        Throwable current = exception;

        while (current != null) {
            String message = current.getMessage();
            if (message != null && !message.isBlank()) {
                if (!sb.isEmpty()) {
                    sb.append("\n  caused by: ");
                }
                if (current instanceof BusinessException businessException) {
                    sb.append(businessException.getErrorCode().getCode()).append(": ").append(message);
                } else {
                    sb.append(current.getClass().getSimpleName()).append(": ").append(message);
                }
            }
            current = current.getCause();
        }

        return sb.isEmpty() ? fallback : sb.toString();
    }

    public String failureReasonCode(Throwable exception, String fallbackReasonCode) {
        return errorCode(exception)
                .map(ErrorCode::getCode)
                .orElse(fallbackReasonCode);
    }

    public Map<String, Object> failureMetadata(Throwable exception) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        errorCode(exception).ifPresent(errorCode -> {
            metadata.put("clientErrorCode", errorCode.getCode());
            metadata.put("httpStatus", errorCode.getStatus().value());
        });
        return metadata;
    }

    public Optional<ErrorCode> errorCode(Throwable exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof BusinessException businessException) {
                return Optional.of(businessException.getErrorCode());
            }
            current = current.getCause();
        }
        return Optional.empty();
    }
}
