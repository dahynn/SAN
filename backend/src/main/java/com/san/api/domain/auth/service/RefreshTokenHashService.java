package com.san.api.domain.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

/** Redis에 refresh token 원문 대신 저장할 해시 값을 생성합니다. */
@Component
public class RefreshTokenHashService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final SecretKeySpec secretKeySpec;

    public RefreshTokenHashService(@Value("${jwt.secret}") String secret) {
        this.secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
    }

    public String hash(String refreshToken) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(secretKeySpec);
            byte[] hashed = mac.doFinal(refreshToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (Exception e) {
            throw new IllegalStateException("Refresh token hash generation failed.", e);
        }
    }
}
