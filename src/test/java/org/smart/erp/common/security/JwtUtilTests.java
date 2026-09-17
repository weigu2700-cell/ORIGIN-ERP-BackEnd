package org.smart.erp.common.security;

import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtUtilTests {

    private static final long EXPIRATION_MILLIS = 60_000;

    @Test
    void tokenCanOnlyBeParsedWithConfiguredSecret() {
        JwtUtil issuer = new JwtUtil(encodedSecret("0123456789abcdef0123456789abcdef"), EXPIRATION_MILLIS);
        JwtUtil sameSecret = new JwtUtil(encodedSecret("0123456789abcdef0123456789abcdef"), EXPIRATION_MILLIS);
        JwtUtil differentSecret = new JwtUtil(encodedSecret("abcdef0123456789abcdef0123456789"), EXPIRATION_MILLIS);

        String token = issuer.generateToken(42L);

        assertThat(sameSecret.parseToken(token)).isEqualTo(42L);
        assertThatThrownBy(() -> differentSecret.parseToken(token))
                .isInstanceOf(SignatureException.class);
    }

    private String encodedSecret(String rawSecret) {
        return Base64.getEncoder().encodeToString(rawSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
