package org.smart.erp.tenant;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.spring.MybatisSqlSessionFactoryBean;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.mybatis.spring.SqlSessionTemplate;
import org.smart.erp.common.security.*;
import org.smart.erp.system.controller.LoginController;
import org.smart.erp.system.mapper.UserMapper;
import org.smart.erp.system.service.*;
import org.smart.erp.system.service.impl.LoginServiceImpl;
import org.smart.erp.tenant.config.TenantProperties;
import org.smart.erp.tenant.context.TenantScope;
import org.smart.erp.tenant.datasource.*;
import org.smart.erp.tenant.registry.*;
import org.smart.erp.tenant.web.*;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** 只在显式指定的临时 MySQL 中运行，不回退业务库。 */
@EnabledIfEnvironmentVariable(named="TENANT_TEST_JDBC_BASE", matches="jdbc:mysql://.+/")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TenantIsolationTests {
    private final String prefix = "tenant_verify_" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0,8) + "_";
    private MockMvc mvc;
    private JdbcTemplate platform;
    private TenantRoutingDataSource routed;
    private TenantDataSourceRegistry pools;
    private PermissionService permissions;
    private JwtUtil jwt;
    private final ObjectMapper json = new ObjectMapper();
    private String tokenA, tokenB;
    private String url(String schema) { return System.getenv("TENANT_TEST_JDBC_BASE") + (schema.isEmpty() ? "" : prefix + schema) + "?useSSL=false&allowPublicKeyRetrieval=true"; }
    private DriverManagerDataSource source(String schema) { return new DriverManagerDataSource(url(schema),"root",""); }

    @BeforeAll void setup() throws Exception {
        var admin = new JdbcTemplate(source(""));
        for (String schema : new String[]{"tenant_platform", "tenant_a", "tenant_b"}) admin.execute("CREATE DATABASE " + prefix + schema);
        platform = new JdbcTemplate(source("tenant_platform"));
        for (String sql : Files.readString(Path.of("sql/platform/01_tenant_registry.sql")).replaceAll("(?m)--.*$", "").split(";"))
            if (!sql.isBlank()) platform.execute(sql);
        var encoder = new BCryptPasswordEncoder();
        for (String key : new String[]{"a","b"}) {
            var jdbc = new JdbcTemplate(source("tenant_" + key));
            jdbc.execute("CREATE TABLE sys_user (id BIGINT PRIMARY KEY,username VARCHAR(50),password VARCHAR(255),phone VARCHAR(20),status TINYINT,create_time DATETIME,update_time DATETIME,deleted TINYINT,dept_id BIGINT,real_name VARCHAR(50))");
            jdbc.update("INSERT INTO sys_user VALUES(80001,'operator',?,NULL,1,NOW(),NULL,0,NULL,?)", encoder.encode("isolation-test"), key);
            jdbc.execute("CREATE TABLE tenant_probe(id BIGINT PRIMARY KEY, marker VARCHAR(64))");
            jdbc.update("INSERT INTO tenant_probe VALUES(1,?)", key);
            jdbc.execute("CREATE TABLE sys_tenant_identity(singleton TINYINT PRIMARY KEY,tenant_id VARCHAR(64) UNIQUE)");
            jdbc.update("INSERT INTO sys_tenant_identity VALUES(1,?)",key);
            platform.update("INSERT INTO plt_tenant(id,code,name,status) VALUES(?,?,?,'ACTIVE')",key,key,"企业"+key);
            platform.update("INSERT INTO plt_tenant_database(tenant_id,connection_ref,schema_status,schema_version) VALUES(?,?,'READY','3')",key,key);
        }
        var props = new TenantProperties(); props.setMode(TenantProperties.Mode.MULTI);
        for (String key : new String[]{"a","b"}) {
            var connection = new TenantProperties.Connection(); connection.setUrl(url("tenant_"+key)); connection.setUsername("root"); connection.setPassword("");
            props.getConnections().put(key,connection);
        }
        var catalog = new JdbcTenantCatalog(source("tenant_platform"));
        var beans = new StaticListableBeanFactory(Map.of("catalog",catalog));
        var resolver = new TenantResolver(props, beans.getBeanProvider(TenantCatalog.class));
        pools = new TenantDataSourceRegistry(props); routed = new TenantRoutingDataSource(catalog,pools);
        var factoryBean = new MybatisSqlSessionFactoryBean(); factoryBean.setDataSource(routed); factoryBean.setConfiguration(new MybatisConfiguration());
        var factory = factoryBean.getObject(); factory.getConfiguration().addMapper(UserMapper.class);
        var mapper = new SqlSessionTemplate(factory).getMapper(UserMapper.class);
        var users = mock(UserService.class);
        when(users.detailUserById(anyLong())).thenAnswer(invocation -> mapper.selectById((Long)invocation.getArgument(0)));
        permissions = mock(PermissionService.class);
        when(permissions.detailCurrentUserPermission(anyLong())).thenAnswer(call -> {
            assertThat(TenantScope.required()).isIn("a", "b");
            return java.util.List.of();
        });
        jwt = new JwtUtil(Base64.getEncoder().encodeToString("tenant-test-secret-must-have-at-least-32-bytes".getBytes()),60000);
        var login = new LoginServiceImpl(mapper,jwt,encoder,resolver);
        mvc = MockMvcBuilders.standaloneSetup(new LoginController(login),new TenantController(resolver),new TenantAccessController(resolver,props,new CurrentUserImpl(),users),new ProbeController(routed))
                .setControllerAdvice(new org.smart.erp.common.exception.GlobalException(), new TenantExceptionHandler())
                .addFilters(new JwtAuthenticationFilter(jwt,users,permissions,resolver,props,json)).build();
        tokenA=login("a"); tokenB=login("b");
    }
    private String login(String code) throws Exception {
        String response = mvc.perform(post("/auth/login").contentType("application/json").content("{\"tenantCode\":\""+code+"\",\"username\":\"operator\",\"password\":\"isolation-test\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(response).path("data").path("token").asText();
    }
    @AfterAll void cleanup() {
        if(pools!=null)pools.close();
        var admin = new JdbcTemplate(source(""));
        for (String schema : new String[]{"tenant_platform", "tenant_a", "tenant_b"}) admin.execute("DROP DATABASE IF EXISTS " + prefix + schema);
    }

    @Test void identicalUserIdsReadAndWriteTheirOwnDatabase() throws Exception {
        assertThat(jwt.parseIdentity(tokenA).userId()).isEqualTo(80001);
        assertThat(jwt.parseIdentity(tokenB).userId()).isEqualTo(80001);
        mvc.perform(get("/tenant-verification/value").header("Authorization","Bearer "+tokenA)).andExpect(status().isOk()).andExpect(content().string("a"));
        mvc.perform(get("/tenant-verification/value").header("Authorization","Bearer "+tokenB)).andExpect(status().isOk()).andExpect(content().string("b"));
        mvc.perform(post("/tenant-verification/value").header("Authorization","Bearer "+tokenA)).andExpect(status().isOk());
        assertThat(new JdbcTemplate(source("tenant_a")).queryForObject("SELECT marker FROM tenant_probe WHERE id=1",String.class)).isEqualTo("a-updated");
        assertThat(new JdbcTemplate(source("tenant_b")).queryForObject("SELECT marker FROM tenant_probe WHERE id=1",String.class)).isEqualTo("b");
        verify(permissions, atLeastOnce()).detailCurrentUserPermission(80001L);
        assertThat(TenantScope.current()).isNull();
    }
    @Test void rejectsConflictingHeaderAndLegacyTokens() throws Exception {
        mvc.perform(get("/tenant-verification/value").header("Authorization","Bearer "+tokenA).header("X-Tenant-ID","b")).andExpect(status().isForbidden());
        mvc.perform(get("/tenant-verification/value").header("Authorization","Bearer "+jwt.generateToken(80001))).andExpect(status().isUnauthorized());
        mvc.perform(get("/tenant-verification/value")).andExpect(status().isUnauthorized());
        assertThat(TenantScope.current()).isNull();
    }
    @Test void publicResolutionDoesNotExposeConnections() throws Exception {
        mvc.perform(post("/auth/tenant/resolve").contentType("application/json").content("{\"tenantCode\":\"a\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.code").value("a")).andExpect(jsonPath("$.data.connectionRef").doesNotExist());
        mvc.perform(post("/auth/tenant/resolve").contentType("application/json").content("{}" )).andExpect(status().isBadRequest());
    }
    @Test void organizationSessionIsAuthenticatedScopedAndDoesNotOpenBusinessAccess() throws Exception {
        mvc.perform(get("/auth/tenant/context")).andExpect(status().isOk()).andExpect(jsonPath("$.data.mode").value("multi"))
                .andExpect(jsonPath("$.data.organization").isEmpty());
        mvc.perform(get("/auth/tenant/session")).andExpect(status().isUnauthorized());
        for(var pair:java.util.Map.of("a",tokenA,"b",tokenB).entrySet()) {
            mvc.perform(get("/auth/tenant/session").header("Authorization","Bearer "+pair.getValue()))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.data.tenantId").value(pair.getKey()))
                    .andExpect(jsonPath("$.data.organization.code").value(pair.getKey()))
                    .andExpect(jsonPath("$.data.user.id").value("80001"))
                    .andExpect(jsonPath("$.data.user.password").doesNotExist())
                    .andExpect(jsonPath("$.data.businessAvailable").value(false));
        }
        mvc.perform(get("/system/menu/current").header("Authorization","Bearer "+tokenA)).andExpect(status().isServiceUnavailable());
    }
    @Test void suspendedTenantFailsWithoutAffectingOtherTenant() throws Exception {
        platform.update("UPDATE plt_tenant SET status='SUSPENDED' WHERE id='a'");
        try {
            mvc.perform(get("/tenant-verification/value").header("Authorization","Bearer "+tokenA)).andExpect(status().isForbidden());
            mvc.perform(get("/tenant-verification/value").header("Authorization","Bearer "+tokenB)).andExpect(status().isOk());
        } finally { platform.update("UPDATE plt_tenant SET status='ACTIVE' WHERE id='a'"); }
    }
    @Test void databaseFailureNeverFallsBackToOtherTenant() throws Exception {
        platform.update("UPDATE plt_tenant_database SET connection_ref='missing',routing_version=2 WHERE tenant_id='a'");
        try {
            mvc.perform(get("/tenant-verification/value").header("Authorization","Bearer "+tokenA)).andExpect(status().isServiceUnavailable());
            mvc.perform(post("/auth/login").contentType("application/json").content("{\"tenantCode\":\"a\",\"username\":\"operator\",\"password\":\"isolation-test\"}"))
                    .andExpect(status().isServiceUnavailable());
            mvc.perform(get("/tenant-verification/value").header("Authorization","Bearer "+tokenB)).andExpect(status().isOk());
        } finally { platform.update("UPDATE plt_tenant_database SET connection_ref='a',routing_version=1 WHERE tenant_id='a'"); }
    }
    @Test void incompatibleSchemaVersionIsRejectedBeforeBusinessAccess() throws Exception {
        platform.update("UPDATE plt_tenant_database SET schema_version='999' WHERE tenant_id='a'");
        try {
            mvc.perform(get("/tenant-verification/value").header("Authorization","Bearer "+tokenA)).andExpect(status().isServiceUnavailable());
            mvc.perform(get("/tenant-verification/value").header("Authorization","Bearer "+tokenB)).andExpect(status().isOk());
        } finally { platform.update("UPDATE plt_tenant_database SET schema_version='3' WHERE tenant_id='a'"); }
    }
    @Test void missingScopeAndPrematureBusinessAccessAreBlocked() throws Exception {
        assertThatThrownBy(routed::getConnection).isInstanceOf(TenantException.class);
        mvc.perform(get("/system/menu/current").header("Authorization","Bearer "+tokenA)).andExpect(status().isServiceUnavailable());
        mvc.perform(get("/ai/assistant/chat").header("Authorization","Bearer "+tokenA)).andExpect(status().isServiceUnavailable());
        mvc.perform(get("/ws/notification?token="+tokenA)).andExpect(status().isServiceUnavailable());
    }
    @org.springframework.context.annotation.Profile("tenant-fixture")
    @RestController static class ProbeController {
        private final JdbcTemplate jdbc;
        private final TransactionTemplate tx;
        ProbeController(TenantRoutingDataSource source) { jdbc=new JdbcTemplate(source);tx=new TransactionTemplate(new JdbcTransactionManager(source)); }
        @GetMapping("/tenant-verification/value") String read() { return jdbc.queryForObject("SELECT marker FROM tenant_probe WHERE id=1",String.class); }
        @PostMapping("/tenant-verification/value") String write() {
            return tx.execute(status -> {
                assertThatThrownBy(() -> TenantScope.open("b")).isInstanceOf(TenantException.class);
                jdbc.update("UPDATE tenant_probe SET marker='a-updated' WHERE id=1"); return "ok";
            });
        }
    }
}
