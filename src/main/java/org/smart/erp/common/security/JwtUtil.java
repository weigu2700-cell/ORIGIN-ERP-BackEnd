package org.smart.erp.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtUtil {

    private final SecretKey secretKey;
    private final long expirationMillis;

    public JwtUtil(
            @Value("${security.jwt.secret}") String encodedSecret,
            @Value("${security.jwt.expiration-millis:18000000}") long expirationMillis) {
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(encodedSecret));
        this.expirationMillis = expirationMillis;
    }

    public record Identity(long userId, String tenantId) {}

    public String generateToken(long userId) { return generateToken(userId, null); }

    public String generateToken(long userId, String tenantId) {
        var builder = Jwts.builder().subject(String.valueOf(userId))
                .issuedAt(new Date()).expiration(new Date(System.currentTimeMillis() + expirationMillis));
        if (tenantId != null) builder.claim("tenantId", tenantId).claim("ver", 1).audience().add("tenant-api").and();
        return builder.signWith(secretKey).compact();
    }

    public Identity parseIdentity(String token) {
        Claims claims = Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload();
        String tenantId = claims.get("tenantId", String.class);
        if (tenantId == null && (claims.getAudience() != null || claims.get("ver") != null))
            throw new io.jsonwebtoken.MalformedJwtException("登录身份缺少企业信息");
        if (tenantId != null && (tenantId.isBlank() || !Integer.valueOf(1).equals(claims.get("ver", Integer.class))
                || claims.getAudience() == null || !claims.getAudience().contains("tenant-api")))
            throw new io.jsonwebtoken.MalformedJwtException("登录身份用途或版本不匹配");
        return new Identity(Long.parseLong(claims.getSubject()), tenantId);
    }

    public long parseToken(String token) { return parseIdentity(token).userId(); }
}
