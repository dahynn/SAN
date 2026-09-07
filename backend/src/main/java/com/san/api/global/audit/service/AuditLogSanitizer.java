package com.san.api.global.audit.service;

import com.san.api.global.audit.dto.AuditLogCreateCommand;
import com.san.api.global.audit.entity.AuditOutcome;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

final class AuditLogSanitizer {

    private static final String REDACTED = "[REDACTED]";
    private static final String UNKNOWN_FAILURE = "UNKNOWN_FAILURE";
    private static final String DEFAULT_FAILURE_MESSAGE = "Audit failure occurred.";
    private static final int FAILURE_REASON_CODE_MAX_LENGTH = 100;
    private static final int FAILURE_MESSAGE_MAX_LENGTH = 1_000;
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "\\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}\\b",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern BEARER_TOKEN_PATTERN = Pattern.compile(
            "(?i)bearer\\s+[A-Za-z0-9._~+/=-]+"
    );

    private AuditLogSanitizer() {
    }

    static AuditLogCreateCommand sanitize(AuditLogCreateCommand command) {
        return new AuditLogCreateCommand(
                command.actorUserId(),
                command.traceId(),
                command.eventDomain(),
                command.eventType(),
                command.targetType(),
                command.targetId(),
                command.outcome(),
                sanitizeFailureReasonCode(command),
                sanitizeFailureMessage(command),
                command.ipAddress(),
                command.userAgent(),
                sanitizeMetadata(command.metadata())
        );
    }

    private static String sanitizeFailureReasonCode(AuditLogCreateCommand command) {
        if (command.outcome() != AuditOutcome.FAILURE) {
            return null;
        }

        String code = trimToNull(command.failureReasonCode());
        if (code == null) {
            return UNKNOWN_FAILURE;
        }

        String normalized = code.toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9_.-]", "_");
        if (normalized.length() > FAILURE_REASON_CODE_MAX_LENGTH) {
            return normalized.substring(0, FAILURE_REASON_CODE_MAX_LENGTH);
        }
        return normalized;
    }

    private static String sanitizeFailureMessage(AuditLogCreateCommand command) {
        if (command.outcome() != AuditOutcome.FAILURE) {
            return null;
        }

        String message = trimToNull(command.failureMessage());
        if (message == null) {
            return DEFAULT_FAILURE_MESSAGE;
        }

        String sanitized = maskStringContent(message);
        if (sanitized.length() > FAILURE_MESSAGE_MAX_LENGTH) {
            return sanitized.substring(0, FAILURE_MESSAGE_MAX_LENGTH);
        }
        return sanitized;
    }

    private static Map<String, Object> sanitizeMetadata(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return metadata;
        }

        Map<String, Object> sanitized = new LinkedHashMap<>();
        metadata.forEach((key, value) -> sanitized.put(key, sanitizeValue(key, value)));
        return sanitized;
    }

    private static Object sanitizeValue(String key, Object value) {
        if (value == null) {
            return null;
        }

        if (isSecretKey(key)) {
            return REDACTED;
        }
        if (value instanceof String stringValue) {
            return sanitizeStringValue(key, stringValue);
        }
        if (value instanceof Map<?, ?> mapValue) {
            Map<String, Object> sanitized = new LinkedHashMap<>();
            mapValue.forEach((nestedKey, nestedValue) ->
                    sanitized.put(String.valueOf(nestedKey), sanitizeValue(String.valueOf(nestedKey), nestedValue))
            );
            return sanitized;
        }
        if (value instanceof Collection<?> collectionValue) {
            List<Object> sanitized = new ArrayList<>();
            for (Object item : collectionValue) {
                sanitized.add(sanitizeValue(null, item));
            }
            return sanitized;
        }
        if (value.getClass().isArray()) {
            List<Object> sanitized = new ArrayList<>();
            int length = Array.getLength(value);
            for (int i = 0; i < length; i++) {
                sanitized.add(sanitizeValue(null, Array.get(value, i)));
            }
            return sanitized;
        }
        return value;
    }

    private static String sanitizeStringValue(String key, String value) {
        if (isEmailKey(key)) {
            return maskEmail(value);
        }
        if (isIdentifierKey(key)) {
            return maskIdentifier(value);
        }
        return maskStringContent(value);
    }

    private static boolean isSecretKey(String key) {
        if (key == null) {
            return false;
        }
        String normalizedKey = key.toLowerCase(Locale.ROOT);
        return normalizedKey.contains("password")
                || normalizedKey.contains("token")
                || normalizedKey.contains("authorization")
                || normalizedKey.contains("cookie")
                || normalizedKey.contains("secret")
                || normalizedKey.contains("credential")
                || normalizedKey.contains("sessionid")
                || normalizedKey.contains("session_id")
                || normalizedKey.contains("familyid")
                || normalizedKey.contains("family_id")
                || normalizedKey.equals("jti");
    }

    private static boolean isEmailKey(String key) {
        return key != null && key.toLowerCase(Locale.ROOT).contains("email");
    }

    private static boolean isIdentifierKey(String key) {
        return key != null && key.toLowerCase(Locale.ROOT).contains("username");
    }

    private static String maskStringContent(String value) {
        String withoutBearerToken = BEARER_TOKEN_PATTERN.matcher(value).replaceAll("Bearer " + REDACTED);
        return EMAIL_PATTERN.matcher(withoutBearerToken).replaceAll(matchResult -> maskEmail(matchResult.group()));
    }

    private static String maskEmail(String value) {
        int atIndex = value.indexOf('@');
        if (atIndex <= 0) {
            return maskIdentifier(value);
        }
        String localPart = value.substring(0, atIndex);
        String domain = value.substring(atIndex);
        return maskIdentifier(localPart) + domain;
    }

    private static String maskIdentifier(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        String trimmed = value.trim();
        if (trimmed.length() <= 2) {
            return "*".repeat(trimmed.length());
        }
        return trimmed.substring(0, 2) + "***";
    }

    private static String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
