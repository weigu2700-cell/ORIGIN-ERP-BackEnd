package org.smart.erp.im;

import org.junit.jupiter.api.*;
import org.smart.erp.common.security.JwtUtil;
import org.smart.erp.tenant.migration.TenantFlyway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;

/** 完整应用、真实 JWT/HTTP 握手、Redis Outbox -> Pub/Sub -> WebSocket，无手动触发推送。 */
@Testcontainers
@DirtiesContext
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = { "im.enabled=true", "im.storage.enabled=false", "tenancy.mode=single",
                "tenancy.single-tenant-id=single", "spring.flyway.enabled=false",
                "spring.ai.openai.api-key=im-test-only",
                "spring.ai.chat.memory.repository.jdbc.initialize-schema=never",
                "security.jwt.secret=" + ImIntegrationTests.SECRET, "im.allowed-origins[0]=https://erp.example.test",
                "logging.level.root=WARN" })
class ImHttpWebSocketTests {

    @Container
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.4").withDatabaseName("im_http")
        .withUsername("root")
        .withPassword("im-test-only");

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7.4-alpine").withExposedPorts(6379);

    @LocalServerPort
    int port;

    @Autowired
    JwtUtil jwt;
    static final ImJson JSON = new ImJson();

    final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", MYSQL::getJdbcUrl);
        properties.add("spring.datasource.username", MYSQL::getUsername);
        properties.add("spring.datasource.password", MYSQL::getPassword);
        properties.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
        properties.add("spring.data.redis.host", REDIS::getHost);
        properties.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }

    @BeforeAll
    static void schema() {
        var source = new DriverManagerDataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
        var flyway = TenantFlyway.tenant(source);
        org.flywaydb.core.Flyway.configure().configuration(flyway.getConfiguration()).target("2").load().migrate();
        var jdbc = new JdbcTemplate(source);
        jdbc.update("INSERT INTO sys_tenant_identity(singleton,tenant_id) VALUES(1,'single')");
        flyway.migrate();
        for (int user = 1; user <= 5; user++)
            jdbc.update(
                    "INSERT INTO sys_user(id,username,password,status,create_time,deleted) VALUES(?,?,'not-used-for-jwt-tests',1,NOW(),0)",
                    user, "http-employee" + user);
        jdbc.update(
                "INSERT INTO sys_role(id,name,code,status,create_time,deleted) VALUES(1,'测试管理员','admin',1,NOW(),0)");
        for (int user = 1; user <= 3; user++)
            jdbc.update("INSERT INTO sys_user_role(id,user_id,role_id) VALUES(?,?,1)", user, user);
        jdbc.update("INSERT INTO sys_user_role(id,user_id,role_id) VALUES(5,5,1)");
    }

    Map<String, Object> request(long user, String method, String path, Object body, int expected) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
            .timeout(Duration.ofSeconds(10));
        if (user > 0)
            request.header("Authorization", "Bearer " + jwt.generateToken(user, "single"));
        if (body != null)
            request.header("Content-Type", "application/json");
        request.method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(JSON.write(body), StandardCharsets.UTF_8));
        var response = http.send(request.build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).as(response.body()).isEqualTo(expected);
        return JSON.read(response.body(), Map.class);
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> data(Map<String, Object> response) {
        return (Map<String, Object>) response.get("data");
    }

    static final class Inbox implements WebSocket.Listener {

        final BlockingQueue<String> frames = new LinkedBlockingQueue<>();

        final CompletableFuture<Integer> closed = new CompletableFuture<>();

        final StringBuilder partial = new StringBuilder();

        @Override
        public void onOpen(WebSocket socket) {
            socket.request(1);
        }

        @Override
        public CompletionStage<?> onText(WebSocket socket, CharSequence text, boolean last) {
            partial.append(text);
            if (last) {
                frames.add(partial.toString());
                partial.setLength(0);
            }
            socket.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onClose(WebSocket socket, int code, String reason) {
            closed.complete(code);
            return null;
        }

        String await(String type) throws Exception {
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(15);
            while (System.nanoTime() < deadline) {
                String frame = frames.poll(1, TimeUnit.SECONDS);
                if (frame != null && type.equals(JSON.read(frame, Map.class).get("type")))
                    return frame;
            }
            throw new AssertionError("未收到实时帧 " + type);
        }

    }

    @Test
    void realAuthenticatedHttpAndTicketHandshakeDeliverCommittedMessages() throws Exception {
        request(0, "GET", "/im/config", null, 401);
        request(4, "GET", "/im/config", null, 403);
        var config = data(request(1, "GET", "/im/config", null, 200));
        assertThat(config.get("maxFileBytes")).isEqualTo(50_000_000);
        var group = data(request(1, "POST", "/im/groups", Map.of("clientRequestId", UUID.randomUUID().toString(),
                "name", "HTTP联调群", "memberIds", List.of("2", "3")), 200));
        String conversation = group.get("id").toString();
        var ticket = data(request(2, "POST", "/im/ws-tickets", Map.of("clientId", UUID.randomUUID().toString()), 200));
        URI uri = URI.create("ws://127.0.0.1:" + port + ticket.get("websocketPath"));
        // 拒绝未知网页来源；拒绝时不消费票据，原生连接随后仍可正常认证。
        assertThatThrownBy(() -> http.newWebSocketBuilder()
            .header("Origin", "https://untrusted.example")
            .buildAsync(uri, new Inbox())
            .get(5, TimeUnit.SECONDS)).hasCauseInstanceOf(WebSocketHandshakeException.class);
        var inbox = new Inbox();
        var socket = http.newWebSocketBuilder().buildAsync(uri, inbox).get(10, TimeUnit.SECONDS);
        try {
            assertThat(inbox.await("SESSION.READY")).contains("\"userId\":\"2\"");
            String key = UUID.randomUUID().toString();
            var payload = Map.of("clientMessageId", key, "type", "TEXT", "text", "真实实时消息");
            var sent = data(request(1, "POST", "/im/conversations/" + conversation + "/messages", payload, 200));
            assertThat(inbox.await("MESSAGE.CREATED")).contains("真实实时消息");
            assertThat(data(request(1, "POST", "/im/conversations/" + conversation + "/messages", payload, 200))
                .get("deduplicated")).isEqualTo(true);
            socket.sendText(JSON.write(Map.of("type", "PING", "nonce", UUID.randomUUID().toString())), true)
                .get(5, TimeUnit.SECONDS);
            inbox.await("PONG");
            request(4, "GET", "/im/conversations/" + conversation, null, 403);
            request(5, "GET", "/im/conversations/" + conversation, null, 404);
            request(1, "POST", "/im/conversations/" + conversation + "/messages",
                    Map.of("clientMessageId", UUID.randomUUID().toString(), "type", "SYSTEM", "text", "伪造系统消息"), 400);
            assertThat(sent.get("message")).isInstanceOf(Map.class);
            assertThatThrownBy(() -> http.newWebSocketBuilder().buildAsync(uri, new Inbox()).get(5, TimeUnit.SECONDS))
                .hasCauseInstanceOf(WebSocketHandshakeException.class);
            request(1, "DELETE", "/im/groups/" + conversation + "/members/2?expectedVersion=1", null, 200);
            inbox.await("CONVERSATION.REVOKED");
            request(2, "GET", "/im/conversations/" + conversation, null, 404);
        }
        finally {
            socket.sendClose(1000, "done").get(5, TimeUnit.SECONDS);
        }
    }

}
