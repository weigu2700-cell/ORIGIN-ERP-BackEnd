package org.smart.erp.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;

@Component
public class JwtUtil {

    private final SecretKey secretKey =
            Keys.hmacShaKeyFor(Base64.getDecoder()
                    .decode("c21hcnQtZXJwLXNlY3JldC1rZXktc21hcnQtZXJwLXNlY3JldC1rZXktMDEyMzQ1Ng==")
            );

    public String generateToken(long userid) {

        return Jwts.builder()
                .subject(String.valueOf(userid))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60 * 60 * 5000))
                .signWith(secretKey)
                .compact();
    }

    public long parseToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return Long.parseLong(claims.getSubject());
    }
}
