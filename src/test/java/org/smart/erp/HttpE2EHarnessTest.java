package org.smart.erp;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterAll;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import org.smart.erp.ai.service.AiActionService;
import org.smart.erp.ai.model.AiActionProposal;
import org.smart.erp.ai.enums.AiActionType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import java.nio.file.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import static org.assertj.core.api.Assertions.assertThat;

/** Real HTTP server + isolated MySQL/Redis. Python owns API/browser assertions. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class HttpE2EHarnessTest extends org.smart.erp.support.MySqlIntegrationTest {
    // Only the model provider is simulated; ERP, persistence and HTTP stay real.
    static final HttpServer MODEL = startModel();
    static HttpServer startModel() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/", exchange -> {
                String input = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                boolean stream = input.matches("(?s).*\"stream\"\\s*:\\s*true.*");
                String body = stream
                    ? "data: {\"id\":\"mock\",\"object\":\"chat.completion.chunk\",\"created\":1,\"model\":\"mock\",\"choices\":[{\"index\":0,\"delta\":{\"role\":\"assistant\",\"content\":\"local model reply\"},\"finish_reason\":null}]}\n\ndata: {\"id\":\"mock\",\"object\":\"chat.completion.chunk\",\"created\":1,\"model\":\"mock\",\"choices\":[{\"index\":0,\"delta\":{},\"finish_reason\":\"stop\"}]}\n\ndata: [DONE]\n\n"
                    : "{\"id\":\"mock\",\"object\":\"chat.completion\",\"created\":1,\"model\":\"mock\",\"choices\":[{\"index\":0,\"message\":{\"role\":\"assistant\",\"content\":\"local model reply\"},\"finish_reason\":\"stop\"}],\"usage\":{\"prompt_tokens\":1,\"completion_tokens\":1,\"total_tokens\":2}}";
                byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", stream ? "text/event-stream" : "application/json");
                exchange.sendResponseHeaders(200, bytes.length);
                try (var out = exchange.getResponseBody()) { out.write(bytes); }
            });
            server.start();
            return server;
        } catch (Exception error) { throw new IllegalStateException(error); }
    }
    @AfterAll static void stopModel() { MODEL.stop(0); }
    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7.4-alpine").withExposedPorts(6379);
    @DynamicPropertySource
    static void redis(DynamicPropertyRegistry registry) {
        registry.add("spring.ai.openai.base-url", () -> "http://127.0.0.1:" + MODEL.getAddress().getPort());
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }
    @LocalServerPort int port;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder encoder;
    @Autowired AiActionService aiActionService;

    @Test
    void runPytestAgainstRealServer() throws Exception {
        Path root = Path.of("..").toAbsolutePath().normalize();
        // Import DDL only: never import developer accounts or business rows from SQL dumps.
        try (var files = Files.list(Path.of("sql/smart-erp"))) {
            for (Path file : files.filter(p -> p.toString().endsWith(".sql")).toList()) {
                String ddl = Files.readString(file).replace("`smart-erp`.", "");
                if (file.getFileName().toString().equals("pur_in_stock.sql")) {
                    // Recreate the previous production constraint so the migration is actually exercised.
                    ddl = ddl.replaceFirst("(?i)(in_date\\s+datetime\\s+)null", "$1not null");
                }
                var matcher = Pattern.compile("(?is)create\\s+table\\s+.*?;").matcher(ddl);
                while (matcher.find()) jdbc.execute(matcher.group());
            }
        }
        String nullableQuery = "SELECT IS_NULLABLE FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='pur_in_stock' AND COLUMN_NAME='in_date'";
        assertThat(jdbc.queryForObject(nullableQuery, String.class)).isEqualTo("NO");
        jdbc.execute(Files.readString(Path.of("src/main/resources/db/migration/V20261101__purchase_in_stock_date_nullable.sql")));
        assertThat(jdbc.queryForObject(nullableQuery, String.class)).isEqualTo("YES");
        for (int id : new int[]{1,2}) jdbc.update("INSERT INTO sys_user(id,username,password,real_name,create_time) VALUES(?,?,?,?,NOW())",id,id==1?"e2e_admin":"e2e_reader",encoder.encode("E2e-local-only-123"),"自动化验收");
        jdbc.update("INSERT INTO sys_role(id,name,code,create_time) VALUES(1,'验收管理员','admin',NOW())");
        jdbc.update("INSERT INTO sys_user_role(id,user_id,role_id) VALUES(1,1,1)");
        for (String code : new String[]{"inventory:material-stock:list","inventory:material-stock:get","master:warehouse:list","inventory:material-stock:create","purchase:in:stock:list","production:picking:list","sales:delivery:list","production:order:list","sales:order:list","prd:report:page","factory:create","factory:list","factory:update","factory:get","factory:status:update","master:customer:create","master:customer:list","master:customer:get","master:customer:update","master:customer:status","purchase:demand:create","purchase:demand:list","purchase:demand:get","purchase:demand:approve","purchase:demand:close","purchase:order:create","purchase:order:list","purchase:order:get","purchase:order:update","purchase:order:approve","purchase:order:ship","purchase:order:receive","purchase:order:close","purchase:in:stock:detail","purchase:in:stock:approve","purchase:in:stock:upload","sales:order:create","sales:order:get","sales:order:update","sales:order:delete","sales:order:confirm","sales:order:cancel","sales:delivery:get","sales:delivery:complete","sales:delivery:cancel","sales:delivery:confirm","master:material:create","master:material:list","master:material:get","master:material:update","master:material:status","master:supplier:create","master:supplier:list","master:supplier:get","master:supplier:update","master:supplier:status"}) jdbc.update("INSERT INTO sys_permission(name,code,create_time) VALUES('验收权限',?,NOW())",code);
        // Test admin receives the project's declared permission codes; reader remains unprivileged.
        java.util.Set<String> declaredPermissions = new java.util.TreeSet<>();
        Pattern permissionCode = Pattern.compile("'([a-z][a-z0-9_:.-]*:[a-z0-9_:.-]+)'");
        try (var sources = Files.walk(Path.of("src/main/java"))) {
            for (Path source : sources.filter(p -> p.toString().endsWith(".java")).toList()) {
                var found = permissionCode.matcher(Files.readString(source));
                while (found.find()) declaredPermissions.add(found.group(1));
            }
        }
        for (String code : declaredPermissions) jdbc.update("INSERT IGNORE INTO sys_permission(name,code,create_time) VALUES('验收权限',?,NOW())", code);
        jdbc.update("INSERT INTO md_customer(id,code,name,contact_name,phone,status,create_time) VALUES(51,'E2E-CUSTOMER','验收客户','联系人','13900000005',1,NOW())");
        jdbc.update("INSERT INTO md_supplier(id,code,name,phone,create_time) VALUES(41,'E2E-SUPPLIER','验收供应商','13800000003',NOW())");
        jdbc.update("INSERT INTO md_warehouse(id,name,code,type,factory_id,create_time) VALUES(21,'自动化验收仓','E2E',1,1,NOW())");
        for (int id : new int[]{11,12}) jdbc.update("INSERT INTO md_material(id,code,name,type,unit,create_time) VALUES(?,?,?,1,'件',NOW())",id,id==11?"001234":"E2E-CREATE","自动化物料"+id);
        jdbc.update("INSERT INTO inv_material_stock(id,warehouse_id,material_id,on_hand,reserved,create_time) VALUES(31,21,11,10,2,NOW())");
        // Explicit intermediate-state fixtures for transitions not reachable from earlier empty-list tests.
        jdbc.update("INSERT INTO prd_production_demand(id,demand_no,material_id,warehouse_id,demand_quantity,source_type,source_no,status,create_time) VALUES(9000,'E2E-DEMAND',12,21,1,0,'E2E-SOURCE',0,NOW())");
        jdbc.update("INSERT INTO prd_production_order(id,production_order_no,material_id,warehouse_id,planned_quantity,completed_quantity,status,planned_start_time,planned_end_time,remark,create_time) VALUES(9001,'E2E-READY',12,21,1,0,1,NOW(),NOW(),'E2E-READY',NOW())");
        jdbc.update("INSERT INTO prd_production_order(id,production_order_no,material_id,warehouse_id,planned_quantity,completed_quantity,status,planned_start_time,planned_end_time,remark,create_time) VALUES(9002,'E2E-PICKING',12,21,1,0,1,NOW(),NOW(),'E2E-PICKING',NOW())");
        jdbc.update("INSERT INTO prd_production_picking(id,picking_no,production_order_id,material_id,warehouse_id,planned_quantity,actual_quantity,status,create_time) VALUES(9003,'E2E-DRAFT-PICK',9002,11,21,1,0,0,NOW())");
        jdbc.update("INSERT INTO pur_purchase_demand(id,purchase_demand_no,material_id,purchase_quantity,source_type,source_no,status,create_time) VALUES(9005,'E2E-AI-DEMAND',11,1,1,'E2E-AI',1,NOW())");
        jdbc.update("INSERT INTO pur_purchase_order(id,purchase_order_no,purchase_demand_id,material_id,supplier_id,planned_quantity,unit_price,total_amount,order_date,expected_delivery_date,status,create_time,version) VALUES(9006,'E2E-AI-ORDER',9005,11,41,1,5,5,NOW(),NOW(),0,NOW(),0)");
        String actionToken = aiActionService.createPendingAction(java.util.List.of(new AiActionProposal(
                AiActionType.APPROVE_PURCHASE_ORDER, 9006L, "E2E-AI-ORDER", 0, "approve", "test approval")), 1L, 1L).getFirst().token();
        Path project = root.resolve("origin-erp-tests");
        String suite = System.getProperty("erp.e2e.suite", "all");
        var command = new java.util.ArrayList<>(java.util.List.of(project.resolve(".venv/bin/python").toString(),"-m","pytest","-v","--junitxml=reports/http-e2e/junit.xml","--tracing=retain-on-failure","--screenshot=only-on-failure","--output=reports/http-e2e/browser"));
        command.add(suite.equals("api")?"http_tests/api":suite.equals("browser")?"http_tests/browser":"http_tests");
        Path log = project.resolve("reports/http-e2e/pytest.log");
        Files.createDirectories(log.getParent());
        ProcessBuilder builder = new ProcessBuilder(command).directory(project.toFile()).redirectErrorStream(true).redirectOutput(log.toFile());
        builder.environment().put("ERP_ACTION_TOKEN", actionToken);
        builder.environment().put("ERP_ISOLATED_TEST_RUN", "1");
        builder.environment().put("ERP_API_BASE_URL", "http://127.0.0.1:"+port);
        Process process = builder.start();
        boolean finished = process.waitFor(5, TimeUnit.MINUTES);
        if (!finished) process.destroyForcibly();
        System.out.println(Files.readString(log));
        assertThat(finished).as("pytest must finish within five minutes").isTrue();
        assertThat(process.exitValue()).as("pytest HTTP/browser regression").isZero();
    }
}
