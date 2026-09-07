package com.san.api.global.audit.context;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 요청 시점의 감사 컨텍스트를 엔티티에 함께 저장하기 위한 값 객체입니다.
 *
 * <p>비동기 작업처럼 요청 스레드와 실행 스레드가 분리되는 흐름에서도
 * 요청자, traceId, IP, User-Agent를 안정적으로 추적할 수 있도록 스냅샷으로 보관합니다.</p>
 */
@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuditContextSnapshot {

    @Column(name = "actor_user_id", columnDefinition = "uuid")
    private UUID actorUserId;

    @Column(name = "trace_id", length = 100)
    private String traceId;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "text")
    private String userAgent;

    @Enumerated(EnumType.STRING)
    @Column(name = "requested_by_type", length = 30)
    private AuditRequesterType requestedByType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "request_metadata", columnDefinition = "jsonb")
    private Map<String, Object> requestMetadata;

    @Builder
    public AuditContextSnapshot(
            UUID actorUserId,
            String traceId,
            String ipAddress,
            String userAgent,
            AuditRequesterType requestedByType,
            Map<String, Object> requestMetadata
    ) {
        this.actorUserId = actorUserId;
        this.traceId = traceId;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.requestedByType = requestedByType == null ? AuditRequesterType.SYSTEM : requestedByType;
        this.requestMetadata = copyMetadata(requestMetadata);
    }

    public static AuditContextSnapshot from(
            UUID actorUserId,
            AuditRequesterType requestedByType,
            AuditRequestContext requestContext,
            Map<String, Object> requestMetadata
    ) {
        return AuditContextSnapshot.builder()
                .actorUserId(actorUserId)
                .traceId(requestContext == null ? null : requestContext.traceId())
                .ipAddress(requestContext == null ? null : requestContext.ipAddress())
                .userAgent(requestContext == null ? null : requestContext.userAgent())
                .requestedByType(requestedByType)
                .requestMetadata(requestMetadata)
                .build();
    }

    public static AuditContextSnapshot empty() {
        return AuditContextSnapshot.builder()
                .requestedByType(AuditRequesterType.SYSTEM)
                .build();
    }

    public Optional<AuditRequestContext> toRequestContext() {
        if (!hasText(traceId) && !hasText(ipAddress) && !hasText(userAgent)) {
            return Optional.empty();
        }
        return Optional.of(new AuditRequestContext(traceId, ipAddress, userAgent));
    }

    private Map<String, Object> copyMetadata(Map<String, Object> requestMetadata) {
        if (requestMetadata == null || requestMetadata.isEmpty()) {
            return null;
        }
        return new LinkedHashMap<>(requestMetadata);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
