package com.san.api.global.audit.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.san.api.global.audit.entity.AuditLogEvent;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.Map;

@Component
public class AuditLogIntegrityHasher {

    private final ObjectMapper canonicalObjectMapper;

    public AuditLogIntegrityHasher(ObjectMapper objectMapper) {
        this.canonicalObjectMapper = objectMapper.copy()
                .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
    }

    /**
     * 감사 로그의 주요 필드를 고정된 문자열로 정규화한 뒤 SHA-256 해시를 생성합니다.
     * metadata는 key 순서에 따라 해시가 흔들리지 않도록 canonical JSON으로 직렬화합니다.
     *
     * @param event 무결성 해시를 생성할 감사 로그 이벤트
     * @return 64자 hex 형식의 SHA-256 해시
     */
    public String hash(AuditLogEvent event) {
        // 각 필드는 length-prefix 형식으로 연결해 값 안의 구분자 문자 때문에 해시 입력이 모호해지지 않게 합니다.
        String canonicalPayload = String.join("|",
                value(event.getAuditLogEventId()),
                value(event.getActorUserId()),
                value(event.getTraceId()),
                value(event.getEventDomain()),
                value(event.getEventType()),
                value(event.getTargetType()),
                value(event.getTargetId()),
                value(event.getOutcome()),
                value(event.getFailureReasonCode()),
                value(event.getFailureMessage()),
                value(event.getIpAddress()),
                value(event.getUserAgent()),
                metadata(event.getMetadata()),
                value(event.getOccurredAt() == null ? null : event.getOccurredAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
        );
        return sha256(canonicalPayload);
    }

    private String value(Object value) {
        if (value == null) {
            return "null:";
        }
        String stringValue = String.valueOf(value);
        return stringValue.length() + ":" + stringValue;
    }

    private String metadata(Map<String, Object> metadata) {
        try {
            String json = canonicalObjectMapper.writeValueAsString(metadata);
            return value(json);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("감사 로그 메타데이터 직렬화에 실패했습니다.", e);
        }
    }

    private String sha256(String payload) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available.", e);
        }
    }
}
