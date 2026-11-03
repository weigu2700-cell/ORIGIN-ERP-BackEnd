package org.smart.erp.im.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smart.erp.im.*;
import org.smart.erp.tenant.context.TenantExecutor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** 独立 CHAT 连接表，避免与既有通知载荷混发；连接 IO 复用 Spring 有界并发装饰器。 */
@Component
public class ImSockets extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(ImSockets.class);

    private static final class Connection {

        final WebSocketSession socket;

        final ImTickets.Ticket ticket;

        volatile long lastPing = System.currentTimeMillis();

        long cursor;

        int violations;

        Connection(WebSocketSession socket, ImTickets.Ticket ticket, long cursor) {
            this.socket = socket;
            this.ticket = ticket;
            this.cursor = cursor;
        }

    }

    private final ConcurrentHashMap<String, Connection> connections = new ConcurrentHashMap<>();

    private final ImAccess access;

    private final ImEvents events;

    private final ImRedis redis;

    private final ImJson json;

    private final ImProperties properties;

    private final TenantExecutor tenants;

    public ImSockets(ImAccess access, ImEvents events, ImRedis redis, ImJson json, ImProperties properties,
            TenantExecutor tenants) {
        this.access = access;
        this.events = events;
        this.redis = redis;
        this.json = json;
        this.properties = properties;
        this.tenants = tenants;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        var ticket = (ImTickets.Ticket) session.getAttributes().get("imTicket");
        if (ticket == null) {
            session.close(new CloseStatus(4401, "Unauthenticated"));
            return;
        }
        try {
            if (!redis.claimConnection(ticket.tenantId(), ticket.userId(), ticket.clientId(), session.getId(),
                    properties.getMaxConnectionsPerUser())) {
                session.close(new CloseStatus(1013, "Connection limit"));
                return;
            }
            session.setTextMessageSizeLimit(65536);
            session.setBinaryMessageSizeLimit(1024);
            var socket = new ConcurrentWebSocketSessionDecorator(session, 10000, 65536);
            long high = tenants.forTenant(ticket.tenantId()).call(() -> {
                access.require(ticket.userId(), "im:access");
                return events.high(ticket.userId());
            });
            socket.sendMessage(new TextMessage(json.write(Map.of("type", "SESSION.READY", "protocolVersion", 1,
                    "connectionId", session.getId(), "userId", Long.toString(ticket.userId()), "latestEventSeq",
                    Long.toString(high), "heartbeatSeconds", 30, "idleTimeoutSeconds", 90))));
            connections.put(session.getId(), new Connection(socket, ticket, high));
        }
        catch (RuntimeException | IOException error) {
            redis.releaseConnection(ticket.tenantId(), ticket.userId(), ticket.clientId(), session.getId());
            session.close(new CloseStatus(1013, "Chat unavailable"));
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        var connection = connections.get(session.getId());
        if (connection == null) {
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }
        try {
            var body = json.read(message.getPayload(), Map.class);
            if (body.size() != 2 || !"PING".equals(body.get("type")) || !(body.get("nonce") instanceof String nonce))
                throw ImException.invalid("Only PING is accepted");
            ImValues.uuid(nonce);
            if (!Instant.parse(connection.ticket.jwtExpiresAt()).isAfter(Instant.now())) {
                close(connection, 4401, "Login expired");
                return;
            }
            connection.lastPing = System.currentTimeMillis();
            connection.socket.sendMessage(new TextMessage(json.write(Map.of("type", "PONG", "nonce", nonce))));
        }
        catch (ImException error) {
            connection.socket.sendMessage(new TextMessage(json
                .write(Map.of("type", "PROTOCOL.ERROR", "message", "Only PING with a UUID nonce is accepted"))));
            if (++connection.violations >= 3)
                close(connection, 1008, "Protocol violation");
        }
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        var c = connections.get(session.getId());
        if (c != null)
            close(c, 1008, "Binary messages are not supported");
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        var c = connections.remove(session.getId());
        if (c != null)
            release(c);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        var c = connections.get(session.getId());
        if (c != null)
            close(c, 1013, "Transport error");
    }

    public void wake(String tenant, long user) {
        for (var connection : connections.values()) {
            if (!connection.ticket.tenantId().equals(tenant) || connection.ticket.userId() != user)
                continue;
            synchronized (connection) {
                try {
                    if (!valid(connection))
                        continue;
                    tenants.forTenant(tenant).run(() -> {
                        // 有界追赶；更大的离线积压由客户端 REST 恢复。
                        for (int page = 0; page < 10; page++) {
                            var batch = events.syncUser(user, connection.cursor, 100);
                            for (var event : batch.records()) {
                                try {
                                    connection.socket.sendMessage(new TextMessage(json.write(event)));
                                }
                                catch (IOException e) {
                                    throw new IllegalStateException(e);
                                }
                                connection.cursor = ImValues.seq(event.eventSeq());
                            }
                            if (!batch.hasMore())
                                break;
                        }
                    });
                }
                catch (RuntimeException error) {
                    close(connection, 1013, "Recover using REST");
                }
            }
        }
    }

    @Scheduled(fixedDelay = 10000)
    public void reap() {
        for (var connection : connections.values()) {
            try {
                valid(connection);
            }
            catch (RuntimeException error) {
                close(connection, 1013, "Chat unavailable");
            }
        }
    }

    private boolean valid(Connection c) {
        if (!c.socket.isOpen()) {
            connections.remove(c.socket.getId());
            release(c);
            return false;
        }
        if (!Instant.parse(c.ticket.jwtExpiresAt()).isAfter(Instant.now())) {
            close(c, 4401, "Login expired");
            return false;
        }
        if (System.currentTimeMillis() - c.lastPing > 90000) {
            close(c, 1008, "Heartbeat timeout");
            return false;
        }
        try {
            tenants.forTenant(c.ticket.tenantId()).run(() -> access.require(c.ticket.userId(), "im:access"));
        }
        catch (RuntimeException e) {
            close(c, 4403, "Access revoked");
            return false;
        }
        if (!redis.renewConnection(c.ticket.tenantId(), c.ticket.userId(), c.ticket.clientId(), c.socket.getId())) {
            close(c, 4409, "Client replaced");
            return false;
        }
        return true;
    }

    private void close(Connection c, int code, String reason) {
        connections.remove(c.socket.getId());
        release(c);
        try {
            if (c.socket.isOpen())
                c.socket.close(new CloseStatus(code, reason));
        }
        catch (IOException | RuntimeException ignored) {
            log.debug("IM connection closed");
        }
    }

    private void release(Connection c) {
        try {
            redis.releaseConnection(c.ticket.tenantId(), c.ticket.userId(), c.ticket.clientId(), c.socket.getId());
        }
        catch (RuntimeException ignored) {
            /* Redis 租约到期兜底回收。 */ }
    }

}
