package org.smart.erp.im;

import org.smart.erp.common.security.JwtUtil;
import org.smart.erp.tenant.context.TenantExecutor;
import org.springframework.stereotype.Service;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import static org.smart.erp.im.ImModels.*;

@Service
public class ImTickets {

    public record Ticket(String tenantId, long userId, String clientId, String jwtExpiresAt) {
    }

    private final ImAccess access;

    private final ImRedis redis;

    private final ImJson json;

    private final ImStore db;

    private final JwtUtil jwt;

    private final TenantExecutor tenants;

    private final SecureRandom random = new SecureRandom();

    public ImTickets(ImAccess access, ImRedis redis, ImJson json, ImStore db, JwtUtil jwt, TenantExecutor tenants) {
        this.access = access;
        this.redis = redis;
        this.json = json;
        this.db = db;
        this.jwt = jwt;
        this.tenants = tenants;
    }

    public WsTicket issue(TicketInput request, String authorization) {
        long user = access.actor();
        String client = ImValues.uuid(request.clientId());
        redis.rate(db.tenant(), user, "ticket", 5);
        if (authorization == null || !authorization.startsWith("Bearer "))
            throw new ImException(401, "UNAUTHENTICATED", "请重新登录");
        var identity = jwt.parseIdentity(authorization.substring(7));
        if (identity.userId() != user)
            throw new ImException(401, "UNAUTHENTICATED", "身份不匹配");
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant expires = Instant.now().plusSeconds(60);
        if (identity.expiresAt().isBefore(expires))
            expires = identity.expiresAt();
        redis.put("erp:im:ticket:" + ImJson.sha256(token),
                json.write(new Ticket(db.tenant(), user, client, identity.expiresAt().toString())),
                Duration.between(Instant.now(), expires));
        return new WsTicket(token, expires.toString(), "/ws/chat?ticket=" + token, 30, 90);
    }

    public Ticket consume(String value) {
        access.enabled();
        if (value == null || !value.matches("[A-Za-z0-9_-]{43}"))
            throw invalid();
        String stored = redis.consume("erp:im:ticket:" + ImJson.sha256(value));
        if (stored == null)
            throw invalid();
        var ticket = json.read(stored, Ticket.class);
        if (!Instant.parse(ticket.jwtExpiresAt()).isAfter(Instant.now()))
            throw invalid();
        tenants.forTenant(ticket.tenantId()).run(() -> access.require(ticket.userId(), "im:access"));
        return ticket;
    }

    private ImException invalid() {
        return new ImException(401, "TICKET_INVALID", "连接票据无效或已使用");
    }

}
