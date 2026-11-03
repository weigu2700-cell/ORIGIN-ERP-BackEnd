package org.smart.erp.im;

import org.junit.jupiter.api.*;
import org.smart.erp.common.security.CurrentUser;
import org.smart.erp.common.security.JwtUtil;
import org.smart.erp.im.attachment.*;
import org.smart.erp.im.websocket.*;
import org.smart.erp.tenant.config.TenantProperties;
import org.smart.erp.tenant.context.*;
import org.smart.erp.tenant.migration.*;
import org.smart.erp.tenant.registry.*;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.socket.*;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
import software.amazon.awssdk.auth.credentials.*;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.*;

import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.smart.erp.im.ImModels.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** 真 MySQL/Redis/S3 兼容服务；ClamAV 协议端点是测试替身，不替代生产病毒库验收。 */
@Testcontainers
class ImIntegrationTests {

    @Container
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.4").withDatabaseName("im_a")
        .withUsername("root")
        .withPassword("im-test-only");

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7.4-alpine").withExposedPorts(6379);

    @Container
    static final GenericContainer<?> MINIO = new GenericContainer<>(
            "registry.cn-hangzhou.aliyuncs.com/fastgpt/minio:RELEASE.2025-09-07T16-13-09Z")
        .withEnv("MINIO_ROOT_USER", "im-local-test")
        .withEnv("MINIO_ROOT_PASSWORD", "im-local-test-password")
        .withCommand("server", "/data")
        .withExposedPorts(9000)
        .waitingFor(Wait.forHttp("/minio/health/ready").forPort(9000));
    static final ThreadLocal<Long> ACTOR = ThreadLocal.withInitial(() -> 1L);
    static final ImJson JSON = new ImJson();
    static final String SECRET = "c21hcnQtZXJwLXRlc3Qtc2VjcmV0LW11c3QtYmUtYXQtbGVhc3QtMzItYnl0ZXM=";
    static LettuceConnectionFactory redisFactory;
    static ScannerStub scanner;
    static DriverManagerDataSource sourceA, sourceB;
    static S3Client s3;

    Graph a, b;

    static final class ScannerStub implements AutoCloseable {

        final ServerSocket server;

        final AtomicReference<CountDownLatch> gate = new AtomicReference<>();

        final AtomicReference<CountDownLatch> entered = new AtomicReference<>();

        ScannerStub() throws IOException {
            server = new ServerSocket(0, 50, InetAddress.getLoopbackAddress());
            Thread.startVirtualThread(() -> {
                while (!server.isClosed())
                    try {
                        var socket = server.accept();
                        Thread.startVirtualThread(() -> {
                            try (socket) {
                                var in = new DataInputStream(socket.getInputStream());
                                if (!new String(in.readNBytes(10), StandardCharsets.US_ASCII).equals("zINSTREAM\0"))
                                    return;
                                var all = new ByteArrayOutputStream();
                                int n;
                                while ((n = in.readInt()) != 0) {
                                    if (n < 0 || n > 65536)
                                        return;
                                    all.write(in.readNBytes(n));
                                }
                                var arrived = entered.get();
                                if (arrived != null)
                                    arrived.countDown();
                                var wait = gate.get();
                                if (wait != null)
                                    wait.await(10, TimeUnit.SECONDS);
                                String result = all.toString(StandardCharsets.UTF_8).contains("EICAR")
                                        ? "stream: Eicar-Test-Signature FOUND\0" : "stream: OK\0";
                                socket.getOutputStream().write(result.getBytes(StandardCharsets.UTF_8));
                            }
                            catch (Exception ignored) {
                            }
                        });
                    }
                    catch (IOException ignored) {
                    }
            });
        }

        public void close() throws IOException {
            server.close();
        }

    }

    static final class Graph implements AutoCloseable {

        final TenantProperties tenancy = new TenantProperties();

        final ImProperties p = new ImProperties();

        final JdbcTemplate jdbc;

        final ImStore db;

        final ImAccess access;

        final ImTransactions tx;

        final ImViews views;

        final ImEvents events;

        final ImRedis redis;

        final ImService chat;

        final ImSnapshots snapshots;

        final ImTickets tickets;

        final S3AttachmentStorage storage;

        final AttachmentInspector inspector;

        final ImAttachments files;

        final ImJobs jobs;

        final ImSockets sockets;

        final ImHandshake handshake;

        final ImController controller;

        final JwtUtil jwt = new JwtUtil(SECRET, 3600000);

        final TenantExecutor executor;

        final MockMvc mvc;

        Graph(DriverManagerDataSource source, String tenant) {
            tenancy.setSingleTenantId(tenant);
            p.setEnabled(true);
            p.setSendsPerSecond(1000);
            p.setStorageQuotaBytes(100_000_000);
            p.setAllowedOrigins(List.of("https://erp.example.test"));
            p.getScanner().setHost("127.0.0.1");
            p.getScanner().setPort(scanner.server.getLocalPort());
            p.getStorage().setEnabled(true);
            p.getStorage().setEndpoint("http://" + MINIO.getHost() + ":" + MINIO.getMappedPort(9000));
            p.getStorage().setBucket("im-test");
            p.getStorage().setAccessKey("im-local-test");
            p.getStorage().setSecretKey("im-local-test-password");
            jdbc = new JdbcTemplate(source);
            var keys = new TenantKeyspace(tenancy);
            var resolver = new TenantResolver(tenancy,
                    new DefaultListableBeanFactory().getBeanProvider(TenantCatalog.class));
            executor = new TenantExecutor(keys, resolver);
            CurrentUser current = new CurrentUser() {
                public Long getUserId() {
                    return ACTOR.get();
                }

                public String getUsername() {
                    return "employee" + ACTOR.get();
                }
            };
            db = new ImStore(source, keys);
            access = new ImAccess(db, current, p, tenancy);
            tx = new ImTransactions(new JdbcTransactionManager(source));
            views = new ImViews(db, JSON);
            events = new ImEvents(db, JSON, access, views, tx);
            redis = new ImRedis(redisFactory);
            chat = new ImService(db, access, views, events, JSON, tx, p, redis);
            snapshots = new ImSnapshots(db, access, views, events, tx, redis, JSON);
            tickets = new ImTickets(access, redis, JSON, db, jwt, executor);
            storage = new S3AttachmentStorage(p);
            inspector = new AttachmentInspector(p);
            files = new ImAttachments(db, access, views, tx, JSON, p, redis, storage, inspector);
            jobs = new ImJobs(p, tenancy, executor, tx, db, redis, JSON, files);
            sockets = new ImSockets(access, events, redis, JSON, p, executor);
            handshake = new ImHandshake(tickets, p);
            controller = new ImController(chat, snapshots, events, tickets, files, JSON);
            mvc = MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new ImErrors()).build();
        }

        public void close() {
            storage.close();
            inspector.close();
        }

    }

    static DriverManagerDataSource source(String name) {
        return new DriverManagerDataSource("jdbc:mysql://" + MYSQL.getHost() + ":" + MYSQL.getMappedPort(3306) + "/"
                + name
                + "?useSSL=false&allowPublicKeyRetrieval=true&connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true",
                "root", "im-test-only");
    }

    static void migrate(DriverManagerDataSource source, String tenant) throws Exception {
        var flyway = TenantFlyway.tenant(source);
        org.flywaydb.core.Flyway.configure().configuration(flyway.getConfiguration()).target("2").load().migrate();
        var jdbc = new JdbcTemplate(source);
        jdbc.update("INSERT INTO sys_tenant_identity(singleton,tenant_id) VALUES(1,?)", tenant);
        flyway.migrate();
        SchemaContract.verify(source, "tenant-v5");
        for (int user = 1; user <= 5; user++) {
            jdbc.update(
                    "INSERT INTO sys_user(id,username,real_name,password,status,create_time,deleted) VALUES(?,?,?,'test-only',1,NOW(),0)",
                    user, "employee" + user, tenant + "员工" + user);
            jdbc.update("INSERT INTO sys_user_role(id,user_id,role_id) VALUES(?,?,100)", user, user);
        }
        jdbc.update(
                "INSERT INTO sys_role(id,name,code,status,create_time,deleted) VALUES(100,'测试管理员','admin',1,NOW(),0)");
    }

    @BeforeAll
    static void start() throws Exception {
        scanner = new ScannerStub();
        redisFactory = new LettuceConnectionFactory(REDIS.getHost(), REDIS.getMappedPort(6379));
        redisFactory.afterPropertiesSet();
        redisFactory.start();
        sourceA = source("im_a");
        new JdbcTemplate(sourceA).execute("CREATE DATABASE im_b CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci");
        sourceB = source("im_b");
        migrate(sourceA, "a");
        migrate(sourceB, "b");
        s3 = S3Client.builder()
            .endpointOverride(URI.create("http://" + MINIO.getHost() + ":" + MINIO.getMappedPort(9000)))
            .region(Region.US_EAST_1)
            .credentialsProvider(StaticCredentialsProvider
                .create(AwsBasicCredentials.create("im-local-test", "im-local-test-password")))
            .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
            .build();
        s3.createBucket(r -> r.bucket("im-test"));
        s3.putBucketVersioning(
                r -> r.bucket("im-test").versioningConfiguration(v -> v.status(BucketVersioningStatus.ENABLED)));
    }

    @BeforeEach
    void setup() {
        ACTOR.set(1L);
        scanner.gate.set(null);
        scanner.entered.set(null);
        for (var source : List.of(sourceA, sourceB)) {
            var jdbc = new JdbcTemplate(source);
            var tables = jdbc.queryForList(
                    "SELECT TABLE_NAME FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME LIKE 'im\\_%'",
                    String.class);
            for (String table : tables)
                jdbc.update("DELETE FROM `" + table + "`");
            jdbc.update("UPDATE sys_user SET status=1");
        }
        try (var connection = redisFactory.getConnection()) {
            connection.serverCommands().flushDb();
        }
        a = new Graph(sourceA, "a");
        b = new Graph(sourceB, "b");
    }

    @AfterEach
    void closeGraphs() {
        a.close();
        b.close();
        ACTOR.remove();
    }

    @AfterAll
    static void stop() throws Exception {
        if (s3 != null)
            s3.close();
        if (redisFactory != null)
            redisFactory.destroy();
        if (scanner != null)
            scanner.close();
    }

    static <T> T as(long user, Supplier<T> action) {
        long before = ACTOR.get();
        try {
            ACTOR.set(user);
            return action.get();
        }
        finally {
            ACTOR.set(before);
        }
    }

    static String uuid() {
        return UUID.randomUUID().toString();
    }

    Conversation group() {
        return a.chat.createGroup(new CreateGroup(uuid(), "装配一组", List.of("2", "3")));
    }

    SendResult text(long conversation, String text) {
        return a.chat.send(conversation, new SendMessage(uuid(), "TEXT", text, null));
    }

    static long id(Conversation conversation) {
        return Long.parseLong(conversation.id());
    }

    static void error(int status, String code, org.assertj.core.api.ThrowableAssert.ThrowingCallable work) {
        assertThatThrownBy(work).isInstanceOfSatisfying(ImException.class, e -> {
            assertThat(e.status()).isEqualTo(status);
            assertThat(e.error()).isEqualTo(code);
        });
    }

    @Test
    void tenantIsolationContactsAndNoAdminMembershipBypass() {
        var one = group();
        text(id(one), "仅A可见");
        var two = b.chat.createGroup(new CreateGroup(uuid(), "B组织", List.of("2", "3")));
        assertThat(b.chat.history(id(two), null, null, 100).records()).noneMatch(m -> "仅A可见".equals(m.text()));
        assertThat(a.chat.contacts(null, null, 100).records()).allMatch(c -> c.displayName().startsWith("a员工"));
        error(404, "RESOURCE_NOT_FOUND", () -> as(4, () -> a.chat.conversation(id(one))));
        try (var scope = TenantScope.open("b")) {
            assertThatThrownBy(() -> a.chat.config()).isInstanceOf(org.smart.erp.tenant.TenantException.class);
        }
    }

    @Test
    void concurrentDirectAndMessageRetriesArePersistentlyIdempotent() throws Exception {
        try (var pool = Executors.newVirtualThreadPerTaskExecutor()) {
            var tasks = new ArrayList<Future<Conversation>>();
            for (int i = 0; i < 16; i++)
                tasks.add(pool.submit(() -> a.chat.direct(new CreateDirect("2"))));
            var ids = new HashSet<String>();
            for (var task : tasks)
                ids.add(task.get(20, TimeUnit.SECONDS).id());
            assertThat(ids).hasSize(1);
            long conversation = Long.parseLong(ids.iterator().next());
            String key = uuid();
            var sends = new ArrayList<Future<SendResult>>();
            for (int i = 0; i < 16; i++)
                sends.add(pool.submit(() -> a.chat.send(conversation, new SendMessage(key, "TEXT", "只落一条", null))));
            var messages = new HashSet<String>();
            for (var task : sends)
                messages.add(task.get(20, TimeUnit.SECONDS).message().id());
            assertThat(messages).hasSize(1);
            assertThat(a.chat.history(conversation, null, null, 100).records()).hasSize(1);
            assertThat(as(2, () -> a.chat.conversation(conversation)).unreadCount()).isEqualTo(1);
            error(409, "IDEMPOTENCY_CONFLICT",
                    () -> a.chat.send(conversation, new SendMessage(key, "TEXT", "改过的内容", null)));
        }
    }

    @Test
    void messageSequenceUnreadAndReadWatermarks() throws Exception {
        long conversation = id(group());
        try (var pool = Executors.newVirtualThreadPerTaskExecutor()) {
            var tasks = new ArrayList<Future<SendResult>>();
            for (int i = 0; i < 12; i++)
                tasks.add(pool.submit(() -> text(conversation, "并发消息")));
            for (var task : tasks)
                task.get(20, TimeUnit.SECONDS);
        }
        var history = a.chat.history(conversation, null, null, 100);
        assertThat(history.records().stream().map(m -> Long.parseLong(m.seq())).toList())
            .containsExactlyElementsOf(java.util.stream.LongStream.rangeClosed(1, 13).boxed().toList());
        assertThat(a.chat.conversation(conversation).unreadCount()).isZero();
        assertThat(as(2, () -> a.chat.conversation(conversation)).unreadCount()).isEqualTo(12);
        as(2, () -> a.chat.read(conversation, new ReadInput("8")));
        var read = as(2, () -> a.chat.read(conversation, new ReadInput("4")));
        assertThat(read.lastReadSeq()).isEqualTo("8");
        assertThat(read.unreadCount()).isEqualTo(5);
        error(400, "INVALID_ARGUMENT", () -> as(2, () -> a.chat.read(conversation, new ReadInput("99"))));
        assertThat(a.chat.history(conversation, "8", null, 2).records()).extracting(Message::seq)
            .containsExactly("6", "7");
        assertThat(a.chat.history(conversation, null, "8", 2).records()).extracting(Message::seq)
            .containsExactly("9", "10");
    }

    @Test
    void groupRolesHistoryRevocationAndRejoinWindow() {
        var group = group();
        long conversation = id(group);
        text(conversation, "入群前机密");
        group = a.chat.add(conversation, new AddMembers(List.of("4"), group.version()));
        assertThat(as(4, () -> a.chat.history(conversation, null, null, 100)).records())
            .noneMatch(m -> "入群前机密".equals(m.text()));
        final int version = group.version();
        error(403, "GROUP_ROLE_REQUIRED",
                () -> as(4, () -> a.chat.rename(conversation, new UpdateGroup("越权", version))));
        a.chat.remove(conversation, 4, version);
        error(404, "RESOURCE_NOT_FOUND", () -> as(4, () -> a.chat.history(conversation, null, null, 100)));
        var redacted = as(4, () -> a.events.sync(0, 100));
        assertThat(redacted.records()).anyMatch(e -> e.type().equals("CONVERSATION.REVOKED"));
        assertThat(JSON.write(redacted)).doesNotContain("入群前机密");
        var current = a.chat.conversation(conversation);
        a.chat.add(conversation, new AddMembers(List.of("4"), current.version()));
        var restored = as(4, () -> a.chat.history(conversation, null, null, 100));
        assertThat(restored.records()).hasSize(1);
        assertThat(restored.records().getFirst().system().action()).isEqualTo("MEMBERS_ADDED");
        error(409, "VERSION_CONFLICT", () -> a.chat.rename(conversation, new UpdateGroup("过期覆盖", 1)));
    }

    @Test
    void transferLeaveDissolveAndIdempotentRetryInReadOnlyGroup() {
        var group = group();
        long conversation = id(group);
        var sent = text(conversation, "原消息");
        error(409, "OWNER_TRANSFER_REQUIRED", () -> a.chat.leave(conversation, new VersionInput(1)));
        group = a.chat.role(conversation, 2, new SetRole("ADMIN", group.version()));
        int version = group.version();
        error(409, "OWNER_TRANSFER_REQUIRED", () -> as(2, () -> a.chat.remove(conversation, 1, version)));
        group = a.chat.transfer(conversation, new TransferOwner("2", version));
        assertThat(group.myRole()).isEqualTo("ADMIN");
        assertThat(group.ownerId()).isEqualTo("2");
        final int dissolveVersion = group.version();
        as(2, () -> a.chat.dissolve(conversation, new VersionInput(dissolveVersion)));
        assertThat(a.chat.send(conversation, new SendMessage(sent.message().clientMessageId(), "TEXT", "原消息", null))
            .deduplicated()).isTrue();
        error(409, "GROUP_NOT_WRITABLE", () -> text(conversation, "新消息"));
        assertThat(a.chat.history(conversation, null, null, 100).records()).anyMatch(m -> "原消息".equals(m.text()));
    }

    @Test
    void createGroupIdempotenceAndAtomicInvalidMembership() {
        String key = uuid();
        var group = a.chat.createGroup(new CreateGroup(key, "项目组", List.of("2", "3")));
        assertThat(a.chat.createGroup(new CreateGroup(key, "项目组", List.of("3", "2"))).id()).isEqualTo(group.id());
        error(409, "IDEMPOTENCY_CONFLICT", () -> a.chat.createGroup(new CreateGroup(key, "不同组", List.of("2", "3"))));
        error(404, "RESOURCE_NOT_FOUND",
                () -> a.chat.add(id(group), new AddMembers(List.of("4", "9999"), group.version())));
        assertThat(a.chat.members(id(group), null, null, 100).records()).hasSize(3);
    }

    @Test
    void snapshotsAreStableAndCannotReuseOldMembershipWindow() {
        var group = group();
        long conversation = id(group);
        a.chat.add(conversation, new AddMembers(List.of("4"), 1));
        var snapshot = as(4, () -> a.snapshots.list(null, null, 1));
        a.chat.remove(conversation, 4, 2);
        a.chat.add(conversation, new AddMembers(List.of("4"), 3));
        error(410, "SNAPSHOT_EXPIRED", () -> as(4, () -> a.snapshots.list(snapshot.snapshotToken(), "0", 1)));
        error(410, "SNAPSHOT_EXPIRED", () -> as(3, () -> a.snapshots.list(snapshot.snapshotToken(), "0", 1)));
        assertThat(snapshot.syncEventSeq()).isNotEqualTo("0");
    }

    @Test
    void eventRecoveryRetentionAndOutboxDispatchRemainRecoverable() {
        long conversation = id(group());
        var first = a.events.sync(0, 1);
        assertThat(first.hasMore()).isTrue();
        text(conversation, "持久补偿");
        assertThat(a.events.sync(Long.parseLong(first.nextAfterEventSeq()), 100).records())
            .anyMatch(e -> e.type().equals("MESSAGE.CREATED"));
        a.jobs.dispatchBatch();
        assertThat(a.jdbc.queryForObject("SELECT COUNT(*) FROM im_outbox WHERE state='DONE'", Integer.class))
            .isGreaterThan(0);
        a.jdbc.update("UPDATE im_user_event SET created_at=DATE_SUB(NOW(),INTERVAL 8 DAY)");
        error(410, "SYNC_CURSOR_EXPIRED", () -> a.events.sync(0, 100));
        a.jobs.prune();
        error(410, "SYNC_CURSOR_EXPIRED", () -> a.events.sync(0, 100));
    }

    @Test
    void ticketsAreSingleUseAndConnectionsAreDistributedLeases() {
        var request = new TicketInput(uuid());
        String authorization = "Bearer " + a.jwt.generateToken(1, "a");
        var ticket = a.tickets.issue(request, authorization);
        assertThat(a.tickets.consume(ticket.ticket()).tenantId()).isEqualTo("a");
        error(401, "TICKET_INVALID", () -> a.tickets.consume(ticket.ticket()));
        String client = uuid();
        assertThat(a.redis.claimConnection("a", 1, client, "old", 1)).isTrue();
        assertThat(a.redis.claimConnection("a", 1, uuid(), "other", 1)).isFalse();
        assertThat(a.redis.claimConnection("a", 1, client, "new", 1)).isTrue();
        assertThat(a.redis.renewConnection("a", 1, client, "old")).isFalse();
        assertThat(a.redis.claimConnection("b", 1, uuid(), "isolated", 1)).isTrue();
    }

    @Test
    void websocketPushUsesPersistedEventsAndRevokesDisabledAccounts() throws Exception {
        long conversation = id(group());
        var sent = new CopyOnWriteArrayList<String>();
        var open = new AtomicBoolean(true);
        WebSocketSession socket = mock(WebSocketSession.class);
        when(socket.getId()).thenReturn("test-connection");
        when(socket.isOpen()).thenAnswer(i -> open.get());
        when(socket.getAttributes()).thenReturn(Map.of("imTicket",
                new ImTickets.Ticket("a", 2, uuid(), java.time.Instant.now().plusSeconds(600).toString())));
        doAnswer(i -> {
            sent.add(((TextMessage) i.getArgument(0)).getPayload());
            return null;
        }).when(socket).sendMessage(any());
        doAnswer(i -> {
            open.set(false);
            return null;
        }).when(socket).close(any());
        a.sockets.afterConnectionEstablished(socket);
        text(conversation, "实时推送");
        a.sockets.wake("a", 2);
        assertThat(sent).anyMatch(s -> s.contains("SESSION.READY")).anyMatch(s -> s.contains("实时推送"));
        a.jdbc.update("UPDATE sys_user SET status=2 WHERE id=2");
        a.sockets.reap();
        verify(socket).close(argThat(status -> status.getCode() == 4403));
    }

    @Test
    void restErrorsAndUnknownFieldsMatchContract() throws Exception {
        a.mvc
            .perform(post("/im/conversations/direct").contentType("application/json")
                .content("{\"peerUserId\":\"2\",\"tenantId\":\"b\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.data.error").value("INVALID_ARGUMENT"));
        a.mvc.perform(post("/im/conversations/direct").contentType("application/json").content("{\"peerUserId\":2}"))
            .andExpect(status().isBadRequest());
        a.mvc.perform(get("/im/sync/events")).andExpect(status().isBadRequest());
        var group = group();
        as(4, () -> {
            try {
                a.mvc.perform(get("/im/conversations/" + group.id())).andExpect(status().isNotFound());
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
            return null;
        });
    }

    static String hash(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }

    UploadReservation upload(long conversation, String name, byte[] bytes) throws Exception {
        var reservation = a.files.reserve(
                new ReserveUpload(uuid(), Long.toString(conversation), name, bytes.length, "text/plain", hash(bytes)));
        assertThat(putBytes(reservation, bytes)).isEqualTo(200);
        return reservation;
    }

    static int putBytes(UploadReservation reservation, byte[] bytes) throws Exception {
        var request = HttpRequest.newBuilder(URI.create(reservation.uploadUrl()))
            .PUT(HttpRequest.BodyPublishers.ofByteArray(bytes));
        reservation.requiredHeaders().forEach((k, v) -> {
            if (!k.equalsIgnoreCase("content-length"))
                request.header(k, v);
        });
        return HttpClient.newHttpClient().send(request.build(), HttpResponse.BodyHandlers.discarding()).statusCode();
    }

    @Test
    void filesUseVerifiedVersionsAndMembershipProtectedRangeDownloads() throws Exception {
        var group = group();
        long conversation = id(group);
        byte[] bytes = "办公附件 ABCDE".getBytes(StandardCharsets.UTF_8);
        var reservation = upload(conversation, "资料.txt", bytes);
        long upload = Long.parseLong(reservation.uploadId());
        assertThat(a.files.complete(upload).status()).isEqualTo("VERIFYING");
        a.files.verifyOne();
        var ready = a.files.get(upload);
        assertThat(ready.status()).isEqualTo("READY");
        a.chat.send(conversation, new SendMessage(uuid(), "FILE", null, ready.file().id()));
        assertThat(putBytes(reservation, bytes)).isEqualTo(200); // 同一个签名仍可写新版本，下载必须固定已检查版本。
        String pinned = a.jdbc.queryForObject("SELECT object_version_id FROM im_file WHERE id=?", String.class,
                ready.file().id());
        String key = a.jdbc.queryForObject("SELECT object_key FROM im_file WHERE id=?", String.class,
                ready.file().id());
        assertThat(s3.headObject(r -> r.bucket("im-test").key(key)).versionId()).isNotEqualTo(pinned);
        try (var downloaded = as(2, () -> a.files.download(Long.parseLong(ready.file().id()), "bytes=0-5"))) {
            assertThat(downloaded.stream().readAllBytes()).isEqualTo(Arrays.copyOfRange(bytes, 0, 6));
        }
        error(416, "INVALID_RANGE", () -> a.files.download(Long.parseLong(ready.file().id()), "bytes=999-1000"));
        a.mvc.perform(get("/im/files/" + ready.file().id() + "/content").header("Range", "bytes=0-2"))
            .andExpect(status().isPartialContent())
            .andExpect(header().string("Content-Range", "bytes 0-2/" + bytes.length));
        a.chat.remove(conversation, 2, group.version());
        error(404, "RESOURCE_NOT_FOUND", () -> as(2, () -> a.files.download(Long.parseLong(ready.file().id()), null)));
        a.jdbc.update("UPDATE im_upload SET signed_until=DATE_SUB(NOW(),INTERVAL 1 MINUTE) WHERE id=?", upload);
        a.files.cleanupOne();
        assertThat(s3.listObjectVersions(r -> r.bucket("im-test").prefix(key)).versions()).hasSize(1);
        assertThat(s3.headObject(r -> r.bucket("im-test").key(key).versionId(pinned)).contentLength())
            .isEqualTo(bytes.length);
    }

    @Test
    void invalidFilesAndCancellationNeverBecomeReadyAndQuotaWaitsForCleanup() throws Exception {
        long conversation = id(group());
        error(413, "FILE_TOO_LARGE", () -> a.files.reserve(new ReserveUpload(uuid(), Long.toString(conversation),
                "large.txt", 50_000_001, "text/plain", "a".repeat(64))));
        error(415, "FILE_TYPE_NOT_ALLOWED", () -> a.files.reserve(new ReserveUpload(uuid(), Long.toString(conversation),
                "script.svg", 10, "image/svg+xml", "a".repeat(64))));
        var invalid = upload(conversation, "fake.png", "not a picture".getBytes(StandardCharsets.UTF_8));
        a.files.complete(Long.parseLong(invalid.uploadId()));
        a.files.verifyOne();
        assertThat(a.files.get(Long.parseLong(invalid.uploadId())).rejectionReason()).isEqualTo("TYPE_MISMATCH");
        var malware = upload(conversation, "test.txt", "EICAR test content".getBytes(StandardCharsets.UTF_8));
        a.files.complete(Long.parseLong(malware.uploadId()));
        a.files.verifyOne();
        assertThat(a.files.get(Long.parseLong(malware.uploadId())).rejectionReason()).isEqualTo("MALWARE_DETECTED");
        var reservation = upload(conversation, "race.txt", "scan then cancel".getBytes(StandardCharsets.UTF_8));
        long upload = Long.parseLong(reservation.uploadId());
        a.files.complete(upload);
        scanner.gate.set(new CountDownLatch(1));
        scanner.entered.set(new CountDownLatch(1));
        try (var pool = Executors.newVirtualThreadPerTaskExecutor()) {
            var worker = pool.submit(a.files::verifyOne);
            assertThat(scanner.entered.get().await(10, TimeUnit.SECONDS)).isTrue();
            a.files.cancel(upload);
            scanner.gate.get().countDown();
            worker.get(20, TimeUnit.SECONDS);
        }
        assertThat(a.files.get(upload).status()).isEqualTo("CANCELLED");
        long before = a.jdbc.queryForObject("SELECT reserved_bytes FROM im_storage_budget", Long.class);
        assertThat(before).isGreaterThan(0);
        a.files.cleanupOne();
        assertThat(a.jdbc.queryForObject("SELECT reserved_bytes FROM im_storage_budget", Long.class)).isEqualTo(before);
        a.jdbc.update("UPDATE im_upload SET signed_until=DATE_SUB(NOW(),INTERVAL 1 MINUTE)");
        for (int i = 0; i < 3; i++)
            a.files.cleanupOne();
        assertThat(a.jdbc.queryForObject("SELECT reserved_bytes FROM im_storage_budget", Long.class)).isZero();
    }

    @Test
    void exactFiftyMbBoundaryAndSignedLengthCannotBeBypassed() throws Exception {
        long conversation = id(group());
        byte[] bytes = new byte[50_000_000];
        Arrays.fill(bytes, (byte) 'A');
        var reservation = upload(conversation, "50MB.txt", bytes);
        long upload = Long.parseLong(reservation.uploadId());
        assertThat(reservation.requiredHeaders()).containsEntry("content-length", "50000000");
        assertThat(putBytes(reservation, new byte[50_000_001])).isNotEqualTo(200);
        a.files.complete(upload);
        a.files.verifyOne();
        assertThat(a.files.get(upload).status()).isEqualTo("READY");
    }

    @Test
    void officeImagesAndArchiveInspectionUseActualContent() throws Exception {
        long conversation = id(group());
        var office = new ByteArrayOutputStream();
        try (var zip = new java.util.zip.ZipOutputStream(office)) {
            var entries = Map.of("[Content_Types].xml",
                    "<?xml version=\"1.0\"?><Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/><Default Extension=\"xml\" ContentType=\"application/xml\"/><Override PartName=\"/word/document.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml\"/></Types>",
                    "_rels/.rels",
                    "<?xml version=\"1.0\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"word/document.xml\"/></Relationships>",
                    "word/document.xml",
                    "<?xml version=\"1.0\"?><w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\"><w:body><w:p><w:r><w:t>办公资料</w:t></w:r></w:p></w:body></w:document>");
            for (var entry : entries.entrySet()) {
                zip.putNextEntry(new java.util.zip.ZipEntry(entry.getKey()));
                zip.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
        var doc = upload(conversation, "办公.docx", office.toByteArray());
        a.files.complete(Long.parseLong(doc.uploadId()));
        a.files.verifyOne();
        assertThat(a.files.get(Long.parseLong(doc.uploadId())).status()).isEqualTo("READY");
        byte[] png = Base64.getDecoder()
            .decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aFGkAAAAASUVORK5CYII=");
        var image = upload(conversation, "图片.png", png);
        a.files.complete(Long.parseLong(image.uploadId()));
        a.files.verifyOne();
        var ready = a.files.get(Long.parseLong(image.uploadId()));
        assertThat(ready.status()).isEqualTo("READY");
        a.chat.send(conversation, new SendMessage(uuid(), "IMAGE", null, ready.file().id()));
        var malicious = new ByteArrayOutputStream();
        try (var zip = new java.util.zip.ZipOutputStream(malicious)) {
            zip.putNextEntry(new java.util.zip.ZipEntry("setup.exe"));
            zip.write(new byte[] { 1, 2, 3 });
            zip.closeEntry();
        }
        var archive = upload(conversation, "不安全.zip", malicious.toByteArray());
        a.files.complete(Long.parseLong(archive.uploadId()));
        a.files.verifyOne();
        assertThat(a.files.get(Long.parseLong(archive.uploadId())).rejectionReason()).isEqualTo("UNSAFE_ARCHIVE");
    }

    @Test
    void writeFailureRollsBackSequenceUnreadAndOutbox() {
        var group = group();
        long conversation = id(group);
        long before = a.events.high(1);
        error(404, "RESOURCE_NOT_FOUND",
                () -> a.chat.send(conversation, new SendMessage(uuid(), "FILE", null, "9999")));
        assertThat(a.chat.conversation(conversation).lastSeq()).isEqualTo("1");
        assertThat(a.events.high(1)).isEqualTo(before);
        assertThat(as(2, () -> a.chat.conversation(conversation)).unreadCount()).isZero();
    }

    @Test
    void outboxExhaustedRetriesAndExpiredLeasesDoNotLoseCommittedMessages() {
        long conversation = id(group());
        text(conversation, "唤醒失败仍可恢复");
        var broken = mock(ImRedis.class);
        doThrow(new IllegalStateException("simulated Redis outage")).when(broken).publish(anyString());
        var dispatcher = new ImJobs(a.p, a.tenancy, a.executor, a.tx, a.db, broken, JSON, a.files);
        for (int attempt = 0; attempt < 10; attempt++) {
            dispatcher.dispatchBatch();
            a.jdbc
                .update("UPDATE im_outbox SET next_attempt_at=DATE_SUB(NOW(),INTERVAL 1 SECOND) WHERE state='PENDING'");
        }
        assertThat(a.jdbc.queryForObject("SELECT COUNT(*) FROM im_outbox WHERE state='FAILED'", Integer.class))
            .isGreaterThan(0);
        assertThat(JSON.write(as(2, () -> a.events.sync(0, 100)))).contains("唤醒失败仍可恢复");
        a.jdbc.update(
                "UPDATE im_outbox SET state='PROCESSING',attempts=0,lease_owner='dead-node',lease_until=DATE_SUB(NOW(),INTERVAL 1 SECOND)");
        a.jobs.dispatchBatch();
        assertThat(a.jdbc.queryForObject("SELECT COUNT(*) FROM im_outbox WHERE state<>'DONE'", Integer.class)).isZero();
    }

    @Test
    void eventPersistenceFailureRollsBackAlreadyInsertedMessageAndUnread() {
        long conversation = id(group());
        long before = a.events.high(1);
        var broken = spy(a.events);
        doThrow(new IllegalStateException("simulated event write failure")).when(broken)
            .append(anyLong(), anyLong(), anyList());
        var service = new ImService(a.db, a.access, a.views, broken, JSON, a.tx, a.p, a.redis);
        assertThatThrownBy(() -> service.send(conversation, new SendMessage(uuid(), "TEXT", "必须回滚", null)))
            .isInstanceOf(IllegalStateException.class);
        assertThat(a.chat.history(conversation, null, null, 100).records()).hasSize(1);
        assertThat(a.chat.conversation(conversation).lastSeq()).isEqualTo("1");
        assertThat(a.events.high(1)).isEqualTo(before);
        assertThat(as(2, () -> a.chat.conversation(conversation)).unreadCount()).isZero();
    }

    @Test
    void contractSamplesAndEndpointInventory() throws Exception {
        var group = group();
        long conversation = id(group);
        text(conversation, "契约样本");
        var samples = new LinkedHashMap<String, Object>();
        samples.put("Config", a.chat.config());
        samples.put("ContactPage", a.chat.contacts(null, null, 100));
        samples.put("Conversation", a.chat.conversation(conversation));
        samples.put("ConversationSnapshot", a.snapshots.list(null, null, 100));
        samples.put("MessagePage", a.chat.history(conversation, null, null, 100));
        samples.put("EventPage", a.events.sync(0, 100));
        samples.put("MemberPage", a.chat.members(conversation, null, null, 100));
        Files.createDirectories(Path.of("target/im"));
        Files.writeString(Path.of("target/im/contract-samples.json"), JSON.write(samples));
        assertThat(Files.readString(Path.of("src/main/resources/db/tenant-migration/V4__employee_realtime_chat.sql")))
            .isEqualTo(
                    Files.readString(Path.of("src/main/resources/db/migration/V20261103__employee_realtime_chat.sql")));
        var paths = new HashSet<String>();
        for (var method : ImController.class.getDeclaredMethods()) {
            for (var annotation : method.getAnnotations()) {
                String verb = switch (annotation.annotationType().getSimpleName()) {
                    case "GetMapping" -> "GET";
                    case "PostMapping" -> "POST";
                    case "PutMapping" -> "PUT";
                    case "DeleteMapping" -> "DELETE";
                    default -> null;
                };
                if (verb != null) {
                    String[] mapped = (String[]) annotation.annotationType().getMethod("value").invoke(annotation);
                    paths.add(verb + " /im" + mapped[0]);
                }
            }
        }
        var spec = JSON.read(Files.readString(Path.of("docs/design/realtime-chat-v1/openapi.json")), Map.class);
        var expected = new HashSet<String>();
        ((Map<String, Map<String, Object>>) spec.get("paths")).forEach((path, methods) -> methods.keySet()
            .stream()
            .filter(m -> Set.of("get", "post", "put", "delete").contains(m))
            .forEach(m -> expected.add(m.toUpperCase(Locale.ROOT) + " " + path)));
        assertThat(paths).containsExactlyInAnyOrderElementsOf(expected);
    }

}
