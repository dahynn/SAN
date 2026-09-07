package com.san.api.domain.auth.service;

import com.san.api.domain.auth.dto.request.DashboardBridgeTokenRequest;
import com.san.api.domain.auth.dto.response.LoginBridgeTicketResponse;
import com.san.api.domain.auth.dto.response.TokenResponse;
import com.san.api.domain.auth.entity.ClientType;
import com.san.api.domain.user.entity.UserRole;
import com.san.api.global.audit.entity.AuditEventType;
import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.AuthErrorCode;
import com.san.api.global.security.redis.AuthRedisKeyPrefix;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardLoginBridgeServiceTest {

    @Mock
    private LoginBridgeTicketService loginBridgeTicketService;

    @Mock
    private TokenIssueService tokenIssueService;

    @Mock
    private AuthAuditService authAuditService;

    private DashboardLoginBridgeService dashboardLoginBridgeService;

    @BeforeEach
    void setUp() {
        dashboardLoginBridgeService = new DashboardLoginBridgeService(
                loginBridgeTicketService,
                tokenIssueService,
                authAuditService
        );
    }

    @Test
    void issueTicketDelegatesExtensionToDashboardBridge() {
        String accessToken = "extension-access-token";
        String userId = UUID.randomUUID().toString();
        LoginBridgeTicketResponse ticketResponse = LoginBridgeTicketResponse.of("dashboard-ticket", 30L);

        when(loginBridgeTicketService.issueTicketWithContext(
                accessToken,
                ClientType.EXTENSION,
                AuthRedisKeyPrefix.LOGIN_BRIDGE_DASHBOARD_TICKET,
                Duration.ofSeconds(30)
        )).thenReturn(new LoginBridgeTicketIssueResult(userId, ClientType.EXTENSION, UserRole.USER, ticketResponse));

        LoginBridgeTicketResponse result = dashboardLoginBridgeService.issueTicket(accessToken);

        assertThat(result).isEqualTo(ticketResponse);
        verify(authAuditService).recordSuccess(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq(AuditEventType.LOGIN_BRIDGE_TICKET_ISSUED),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.argThat(metadata ->
                        "EXTENSION_TO_DASHBOARD".equals(metadata.get("flow"))
                                && "EXTENSION".equals(metadata.get("sourceClientType"))
                                && "DASHBOARD".equals(metadata.get("targetClientType")))
        );
    }

    @Test
    void exchangeTokenConsumesDashboardTicketAndIssuesDashboardTokenPair() {
        String userId = UUID.randomUUID().toString();
        String ticket = "dashboard-ticket";
        TokenResponse tokenResponse = TokenResponse.of("access-token", "refresh-token", 1800L, "dashboard-session");

        when(loginBridgeTicketService.consumeTicketWithContext(ticket, AuthRedisKeyPrefix.LOGIN_BRIDGE_DASHBOARD_TICKET))
                .thenReturn(new LoginBridgeTicketConsumeResult(userId, UserRole.ADMIN));
        when(tokenIssueService.issueTokenPair(userId, ClientType.DASHBOARD, UserRole.ADMIN)).thenReturn(tokenResponse);

        TokenResponse result = dashboardLoginBridgeService.exchangeToken(new DashboardBridgeTokenRequest(ticket));

        assertThat(result).isEqualTo(tokenResponse);
        verify(tokenIssueService).issueTokenPair(userId, ClientType.DASHBOARD, UserRole.ADMIN);
        verify(authAuditService).recordSuccess(
                org.mockito.ArgumentMatchers.argThat(userUuid -> userUuid.toString().equals(userId)),
                org.mockito.ArgumentMatchers.eq(AuditEventType.LOGIN_BRIDGE_TOKEN_EXCHANGED),
                org.mockito.ArgumentMatchers.argThat(userUuid -> userUuid.toString().equals(userId)),
                org.mockito.ArgumentMatchers.argThat(metadata ->
                        "EXTENSION_TO_DASHBOARD".equals(metadata.get("flow"))
                                && "EXTENSION".equals(metadata.get("sourceClientType"))
                                && "DASHBOARD".equals(metadata.get("targetClientType")))
        );
    }

    @Test
    void exchangeTokenRecordsFailureWhenTicketIsInvalid() {
        String ticket = "missing-ticket";
        when(loginBridgeTicketService.consumeTicketWithContext(ticket, AuthRedisKeyPrefix.LOGIN_BRIDGE_DASHBOARD_TICKET))
                .thenThrow(new BusinessException(AuthErrorCode.INVALID_LOGIN_BRIDGE_TICKET));

        assertThatThrownBy(() -> dashboardLoginBridgeService.exchangeToken(new DashboardBridgeTokenRequest(ticket)))
                .isInstanceOf(BusinessException.class);

        verify(authAuditService).recordFailure(
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.eq(AuditEventType.LOGIN_BRIDGE_TOKEN_EXCHANGE_FAILED),
                org.mockito.ArgumentMatchers.eq(AuthErrorCode.INVALID_LOGIN_BRIDGE_TICKET),
                org.mockito.ArgumentMatchers.argThat(metadata ->
                        "EXTENSION_TO_DASHBOARD".equals(metadata.get("flow"))
                                && "EXTENSION".equals(metadata.get("sourceClientType"))
                                && "DASHBOARD".equals(metadata.get("targetClientType")))
        );
    }
}
