package com.san.api.domain.auth.service;

import com.san.api.domain.auth.dto.request.ExtensionBridgeTokenRequest;
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
class ExtensionLoginBridgeServiceTest {

    @Mock
    private LoginBridgeTicketService loginBridgeTicketService;

    @Mock
    private TokenIssueService tokenIssueService;

    @Mock
    private AuthAuditService authAuditService;

    private ExtensionLoginBridgeService extensionLoginBridgeService;

    @BeforeEach
    void setUp() {
        extensionLoginBridgeService = new ExtensionLoginBridgeService(
                loginBridgeTicketService,
                tokenIssueService,
                authAuditService
        );
    }

    @Test
    void issueTicketDelegatesDashboardToExtensionBridge() {
        String accessToken = "dashboard-access-token";
        String userId = UUID.randomUUID().toString();
        LoginBridgeTicketResponse ticketResponse = LoginBridgeTicketResponse.of("bridge-ticket", 120L);

        when(loginBridgeTicketService.issueTicketWithContext(
                accessToken,
                ClientType.DASHBOARD,
                AuthRedisKeyPrefix.LOGIN_BRIDGE_EXTENSION_TICKET,
                Duration.ofMinutes(2)
        )).thenReturn(new LoginBridgeTicketIssueResult(userId, ClientType.DASHBOARD, UserRole.USER, ticketResponse));

        LoginBridgeTicketResponse result = extensionLoginBridgeService.issueTicket(accessToken);

        assertThat(result).isEqualTo(ticketResponse);
        verify(authAuditService).recordSuccess(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq(AuditEventType.LOGIN_BRIDGE_TICKET_ISSUED),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.argThat(metadata ->
                        "DASHBOARD_TO_EXTENSION".equals(metadata.get("flow"))
                                && "DASHBOARD".equals(metadata.get("sourceClientType"))
                                && "EXTENSION".equals(metadata.get("targetClientType")))
        );
    }

    @Test
    void exchangeTokenConsumesExtensionTicketAndIssuesExtensionTokenPair() {
        String userId = UUID.randomUUID().toString();
        String ticket = "bridge-ticket";
        TokenResponse tokenResponse = TokenResponse.of("access-token", "refresh-token", 1800L, "extension-session");

        when(loginBridgeTicketService.consumeTicketWithContext(ticket, AuthRedisKeyPrefix.LOGIN_BRIDGE_EXTENSION_TICKET))
                .thenReturn(new LoginBridgeTicketConsumeResult(userId, UserRole.ADMIN));
        when(tokenIssueService.issueTokenPair(userId, ClientType.EXTENSION, UserRole.ADMIN)).thenReturn(tokenResponse);

        TokenResponse result = extensionLoginBridgeService.exchangeToken(new ExtensionBridgeTokenRequest(ticket));

        assertThat(result).isEqualTo(tokenResponse);
        verify(tokenIssueService).issueTokenPair(userId, ClientType.EXTENSION, UserRole.ADMIN);
        verify(authAuditService).recordSuccess(
                org.mockito.ArgumentMatchers.argThat(userUuid -> userUuid.toString().equals(userId)),
                org.mockito.ArgumentMatchers.eq(AuditEventType.LOGIN_BRIDGE_TOKEN_EXCHANGED),
                org.mockito.ArgumentMatchers.argThat(userUuid -> userUuid.toString().equals(userId)),
                org.mockito.ArgumentMatchers.argThat(metadata ->
                        "DASHBOARD_TO_EXTENSION".equals(metadata.get("flow"))
                                && "DASHBOARD".equals(metadata.get("sourceClientType"))
                                && "EXTENSION".equals(metadata.get("targetClientType")))
        );
    }

    @Test
    void exchangeTokenRecordsFailureWhenTicketIsInvalid() {
        String ticket = "missing-ticket";
        when(loginBridgeTicketService.consumeTicketWithContext(ticket, AuthRedisKeyPrefix.LOGIN_BRIDGE_EXTENSION_TICKET))
                .thenThrow(new BusinessException(AuthErrorCode.INVALID_LOGIN_BRIDGE_TICKET));

        assertThatThrownBy(() -> extensionLoginBridgeService.exchangeToken(new ExtensionBridgeTokenRequest(ticket)))
                .isInstanceOf(BusinessException.class);

        verify(authAuditService).recordFailure(
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.eq(AuditEventType.LOGIN_BRIDGE_TOKEN_EXCHANGE_FAILED),
                org.mockito.ArgumentMatchers.eq(AuthErrorCode.INVALID_LOGIN_BRIDGE_TICKET),
                org.mockito.ArgumentMatchers.argThat(metadata ->
                        "DASHBOARD_TO_EXTENSION".equals(metadata.get("flow"))
                                && "DASHBOARD".equals(metadata.get("sourceClientType"))
                                && "EXTENSION".equals(metadata.get("targetClientType")))
        );
    }
}
