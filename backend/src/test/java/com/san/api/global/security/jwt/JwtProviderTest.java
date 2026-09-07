package com.san.api.global.security.jwt;

import com.san.api.domain.auth.entity.ClientType;
import com.san.api.domain.user.entity.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** JWT에 담긴 사용자 권한이 Spring Security 권한으로 복원되는지 검증합니다. */
class JwtProviderTest {

    private static final String SECRET = "MTIzNDU2Nzg5MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTI=";

    private final JwtProvider jwtProvider = new JwtProvider(SECRET, 1800000L, 604800000L);

    @Test
    void getAuthenticationRestoresAdminAuthorityFromTokenRole() {
        String userId = UUID.randomUUID().toString();
        String token = jwtProvider.generateAccessToken(userId, ClientType.DASHBOARD, "session-id", UserRole.ADMIN);

        var authentication = jwtProvider.getAuthentication(token);

        assertThat(authentication.getPrincipal()).isEqualTo(userId);
        assertThat(authentication.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    void getSessionClaimsContainsUserRole() {
        String token = jwtProvider.generateRefreshToken(
                UUID.randomUUID().toString(),
                ClientType.DASHBOARD,
                "session-id",
                "family-id",
                UserRole.ADMIN
        );

        JwtSessionClaims claims = jwtProvider.getSessionClaims(token);

        assertThat(claims.role()).isEqualTo(UserRole.ADMIN);
    }
}
