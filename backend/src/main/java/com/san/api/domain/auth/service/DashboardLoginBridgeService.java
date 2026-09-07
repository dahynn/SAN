package com.san.api.domain.auth.service;

import com.san.api.domain.auth.dto.request.DashboardBridgeTokenRequest;
import com.san.api.domain.auth.dto.response.LoginBridgeTicketResponse;
import com.san.api.domain.auth.dto.response.TokenResponse;
import com.san.api.domain.auth.entity.ClientType;
import com.san.api.global.audit.entity.AuditEventType;
import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.AuthErrorCode;
import com.san.api.global.security.redis.AuthRedisKeyPrefix;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DashboardLoginBridgeService {

    private static final Duration DASHBOARD_BRIDGE_TICKET_TTL = Duration.ofSeconds(30);

    private final LoginBridgeTicketService loginBridgeTicketService;
    private final TokenIssueService tokenIssueService;
    private final AuthAuditService authAuditService;

    /**
     * 익스텐션 access token을 검증하고 대시보드 로그인에 사용할 일회성 bridge ticket을 발급합니다.
     */
    public LoginBridgeTicketResponse issueTicket(String accessToken) {
        try {
            LoginBridgeTicketIssueResult result = loginBridgeTicketService.issueTicketWithContext(
                    accessToken,
                    ClientType.EXTENSION,
                    AuthRedisKeyPrefix.LOGIN_BRIDGE_DASHBOARD_TICKET,
                    DASHBOARD_BRIDGE_TICKET_TTL
            );
            UUID userId = UUID.fromString(result.userId());
            authAuditService.recordSuccess(
                    userId,
                    AuditEventType.LOGIN_BRIDGE_TICKET_ISSUED,
                    userId,
                    bridgeMetadata()
            );
            return result.response();
        } catch (BusinessException e) {
            recordFailure(AuditEventType.LOGIN_BRIDGE_TICKET_ISSUE_FAILED, e);
            throw e;
        }
    }

    /**
     * 익스텐션에서 발급한 bridge ticket을 소비해 대시보드용 token pair를 발급합니다.
     */
    public TokenResponse exchangeToken(DashboardBridgeTokenRequest request) {
        try {
            LoginBridgeTicketConsumeResult ticket = loginBridgeTicketService.consumeTicketWithContext(
                    request.ticket(),
                    AuthRedisKeyPrefix.LOGIN_BRIDGE_DASHBOARD_TICKET
            );
            String userId = ticket.userId();
            TokenResponse response = tokenIssueService.issueTokenPair(userId, ClientType.DASHBOARD, ticket.role());
            UUID userUuid = UUID.fromString(userId);
            authAuditService.recordSuccess(
                    userUuid,
                    AuditEventType.LOGIN_BRIDGE_TOKEN_EXCHANGED,
                    userUuid,
                    bridgeMetadata()
            );
            return response;
        } catch (BusinessException e) {
            recordFailure(AuditEventType.LOGIN_BRIDGE_TOKEN_EXCHANGE_FAILED, e);
            throw e;
        }
    }

    private void recordFailure(AuditEventType eventType, BusinessException e) {
        if (e.getErrorCode() instanceof AuthErrorCode authErrorCode) {
            authAuditService.recordFailure(null, eventType, authErrorCode, bridgeMetadata());
        }
    }

    private Map<String, Object> bridgeMetadata() {
        return Map.of(
                "flow", "EXTENSION_TO_DASHBOARD",
                "sourceClientType", ClientType.EXTENSION.name(),
                "targetClientType", ClientType.DASHBOARD.name()
        );
    }
}
