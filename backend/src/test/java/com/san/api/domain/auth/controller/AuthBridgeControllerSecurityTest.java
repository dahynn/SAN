package com.san.api.domain.auth.controller;

import com.san.api.domain.auth.dto.response.LoginBridgeTicketResponse;
import com.san.api.domain.auth.dto.response.TokenResponse;
import com.san.api.domain.auth.service.AuthService;
import com.san.api.domain.auth.service.AuthSessionService;
import com.san.api.domain.auth.service.DashboardLoginBridgeService;
import com.san.api.domain.auth.service.ExtensionLoginBridgeService;
import com.san.api.global.security.config.SecurityConfig;
import com.san.api.global.security.filter.JwtAuthenticationFilter;
import com.san.api.global.security.handler.CustomAccessDeniedHandler;
import com.san.api.global.security.handler.CustomAuthenticationEntryPoint;
import com.san.api.global.security.handler.SecurityErrorResponseWriter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({
        SecurityConfig.class,
        SecurityErrorResponseWriter.class,
        CustomAuthenticationEntryPoint.class,
        CustomAccessDeniedHandler.class,
        AuthBridgeControllerSecurityTest.PassThroughJwtFilterConfig.class
})
class AuthBridgeControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private AuthSessionService authSessionService;

    @MockitoBean
    private ExtensionLoginBridgeService extensionLoginBridgeService;

    @MockitoBean
    private DashboardLoginBridgeService dashboardLoginBridgeService;

    @Test
    void exchangeDashboardBridgeTokenIsPublic() throws Exception {
        TokenResponse tokenResponse = TokenResponse.of("dashboard-access", "dashboard-refresh", 1800L, "dashboard-session");
        when(dashboardLoginBridgeService.exchangeToken(org.mockito.ArgumentMatchers.any()))
                .thenReturn(tokenResponse);

        mockMvc.perform(post("/auth/bridge/dashboard-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ticket\":\"dashboard-ticket\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("dashboard-access"))
                .andExpect(jsonPath("$.data.sessionId").value("dashboard-session"));
    }

    @Test
    void issueDashboardBridgeTicketRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/auth/bridge/dashboard-ticket"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void issueDashboardBridgeTicketUsesAuthenticatedRequest() throws Exception {
        LoginBridgeTicketResponse ticketResponse = LoginBridgeTicketResponse.of("dashboard-ticket", 30L);
        when(dashboardLoginBridgeService.issueTicket(null)).thenReturn(ticketResponse);

        mockMvc.perform(post("/auth/bridge/dashboard-ticket"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.ticket").value("dashboard-ticket"))
                .andExpect(jsonPath("$.data.expiresIn").value(30L));

        verify(dashboardLoginBridgeService).issueTicket(null);
    }

    @Test
    void exchangeExtensionBridgeTokenIsPublic() throws Exception {
        TokenResponse tokenResponse = TokenResponse.of("extension-access", "extension-refresh", 1800L, "extension-session");
        when(extensionLoginBridgeService.exchangeToken(org.mockito.ArgumentMatchers.any()))
                .thenReturn(tokenResponse);

        mockMvc.perform(post("/auth/bridge/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ticket\":\"extension-ticket\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("extension-access"))
                .andExpect(jsonPath("$.data.sessionId").value("extension-session"));
    }

    @Test
    void issueExtensionBridgeTicketRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/auth/bridge/ticket"))
                .andExpect(status().isUnauthorized());
    }

    @TestConfiguration
    static class PassThroughJwtFilterConfig {

        @Bean
        JwtAuthenticationFilter jwtAuthenticationFilter() {
            return new JwtAuthenticationFilter(null, null, null) {
                @Override
                protected void doFilterInternal(HttpServletRequest request,
                                                HttpServletResponse response,
                                                FilterChain filterChain) throws ServletException, IOException {
                    filterChain.doFilter(request, response);
                }
            };
        }
    }
}
