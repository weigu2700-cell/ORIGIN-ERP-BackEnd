package org.smart.erp.tenant;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.spring.MybatisSqlSessionFactoryBean;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.mybatis.spring.SqlSessionTemplate;
import org.smart.erp.tenant.context.*;
import org.smart.erp.tenant.config.TenantProperties;
import org.smart.erp.tenant.datasource.*;
import org.smart.erp.tenant.registry.*;
import org.smart.erp.tenant.migration.*;
import org.smart.erp.common.config.MyMetaObjectHandler;
import org.smart.erp.common.security.CurrentUserImpl;
import org.smart.erp.common.websocket.WebSocketSessionManager;
import org.smart.erp.eip.converter.NotificationConverter;
import org.smart.erp.eip.dto.NotificationPublishDTO;
import org.smart.erp.eip.enums.NotificationSourceType;
import org.smart.erp.eip.event.NotificationPublishEvent;
import org.smart.erp.eip.listener.NotificationPublishEventListener;
import org.smart.erp.eip.mapper.*;
import org.smart.erp.eip.service.*;
import org.smart.erp.eip.service.impl.*;
import org.smart.erp.eip.adapter.WebSocketNotificationPushGateway;
import org.smart.erp.ai.config.*;
import org.smart.erp.ai.persistence.*;
import org.smart.erp.ai.request.AiAssistantRequest;
import org.smart.erp.ai.result.*;
import org.smart.erp.ai.service.*;
import org.smart.erp.ai.service.impl.*;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.*;
import org.springframework.ai.chat.messages.*;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.event.TransactionalEventListenerFactory;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;
import java.time.*;
import java.util.*;
import java.util.concurrent.Executors;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@EnabledIfEnvironmentVariable(named="TENANT_TEST_JDBC_BASE", matches="jdbc:mysql://.+/")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TenantAsyncDatabaseTests {
    private final String prefix="tenant_async_"+UUID.randomUUID().toString().replace("-", "").substring(0,8)+"_";
    private TenantDataSourceRegistry pools;
    private TenantRoutingDataSource routing;
    private TenantExecutor executor;
    private DriverManagerDataSource platform;
    private SqlSessionTemplate sql;
    private TenantJdbcChatMemory memory;
    private WebSocketSession aSocket,bSocket;
    private AnnotationConfigApplicationContext events;
    private NotificationPublisher publisher;
    private NotificationPublishEventListener listener;
    private AiMessageService messages;
    private AiConversationService conversations;
    private String url(String name) { return System.getenv("TENANT_TEST_JDBC_BASE")+(name.isEmpty()?"":prefix+name)+"?useSSL=false&allowPublicKeyRetrieval=true"; }
    private DriverManagerDataSource source(String name) { return new DriverManagerDataSource(url(name),"root",""); }
    private JdbcTemplate db(String name) { return new JdbcTemplate(source(name)); }
    @BeforeAll void setup() throws Exception {
        var admin=db("");
        for(String name:List.of("platform","a","b")) admin.execute("CREATE DATABASE "+prefix+name+" CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci");
        platform=source("platform"); PlatformSchemaMigrator.migrate(platform,false);
        var props=new TenantProperties();props.setMode(TenantProperties.Mode.MULTI);
        props.getPlatform().setUrl(url("platform"));
        for(String id:List.of("a","b")) {
            db("platform").update("INSERT INTO plt_tenant(id,code,name) VALUES(?,?,?)",id,id,id);
            db("platform").update("INSERT INTO plt_tenant_database(tenant_id,connection_ref) VALUES(?,?)",id,id);
            new TenantSchemaMigrator(platform).execute(id,id,source(id),TenantSchemaMigrator.Action.NEW,new TenantAdministrator("owner",new BCryptPasswordEncoder().encode("deployment-password")));
            var config=new TenantProperties.Connection();config.setUrl(url(id));config.setUsername("root");config.setPassword("");props.getConnections().put(id,config);
            db(id).update("INSERT INTO ai_conversation(id,user_id,title,status,create_time,update_time,deleted) VALUES(1,80001,'新对话',0,NOW(),NOW(),0)");
        }
        var catalog=new JdbcTenantCatalog(platform);pools=new TenantDataSourceRegistry(props);routing=new TenantRoutingDataSource(catalog,pools);
        executor=new TenantExecutor(TenantTestSupport.multiKeys(),TenantTestSupport.multiResolver(catalog));
        memory=new TenantJdbcChatMemory(routing,executor);
        var factory=new MybatisSqlSessionFactoryBean();factory.setDataSource(routing);factory.setConfiguration(new MybatisConfiguration());
        factory.setGlobalConfig(new GlobalConfig().setMetaObjectHandler(new MyMetaObjectHandler()));
        var sessionFactory=factory.getObject();
        for(var mapper:List.of(NotificationPublishMapper.class,NotificationMapper.class,AiConversationMapper.class,AiMessageMapper.class)) sessionFactory.getConfiguration().addMapper(mapper);
        sql=new SqlSessionTemplate(sessionFactory);
        conversations=new AiConversationServiceImpl(new CurrentUserImpl(),sql.getMapper(AiConversationMapper.class));
        messages=new AiMessageServiceImpl(conversations,sql.getMapper(AiMessageMapper.class));
        var sockets=new WebSocketSessionManager(executor);aSocket=socket("a");bSocket=socket("b");sockets.addSession("a",80001L,aSocket);sockets.addSession("b",80001L,bSocket);
        var gateway=new WebSocketNotificationPushGateway(sockets,new ObjectMapper().registerModule(new JavaTimeModule()));
        var persistence=new NotificationPersistenceServiceImpl(sql.getMapper(NotificationPublishMapper.class),sql.getMapper(NotificationMapper.class),new NotificationConverter(),gateway,executor);
        var proxy=new ProxyFactory(persistence);proxy.setProxyTargetClass(true);
        proxy.addAdvice(new TransactionInterceptor(new JdbcTransactionManager(routing),new AnnotationTransactionAttributeSource()));
        NotificationRecipientResolver recipients=selector -> new LinkedHashSet<>(new JdbcTemplate(routing).queryForList("SELECT id FROM sys_user WHERE status=1",Long.class));
        listener=new NotificationPublishEventListener(recipients,(NotificationPersistenceService)proxy.getProxy(),executor);
        events=new AnnotationConfigApplicationContext();events.registerBean(TransactionalEventListenerFactory.class);
        events.registerBean("notificationListener",NotificationPublishEventListener.class,() -> listener);events.refresh();
        publisher=new NotificationPublisherImpl(events,executor);
    }
    private WebSocketSession socket(String id) {
        var socket=mock(WebSocketSession.class);when(socket.getId()).thenReturn(id);when(socket.isOpen()).thenReturn(true);
        when(socket.getAttributes()).thenReturn(Map.of("expiresAt",Instant.now().plusSeconds(3600)));return socket;
    }
    @BeforeEach void resetAi() {
        for(String id:List.of("a","b")) {db(id).update("DELETE FROM ai_message");db(id).update("DELETE FROM SPRING_AI_CHAT_MEMORY");db(id).update("UPDATE ai_conversation SET title='新对话',status=0 WHERE id=1");}
        clearInvocations(aSocket,bSocket);
    }
    @AfterEach void clearAuth() { SecurityContextHolder.clearContext(); }
    @AfterAll void cleanup() {
        if(events!=null)events.close();if(pools!=null)pools.close();
        for(String name:List.of("a","b","platform"))db("").execute("DROP DATABASE IF EXISTS "+prefix+name);
    }
    @Test void memoryOnReusedWorkerIsScopedAndCannotUseAnotherUsersConversation() throws Exception {
        String aId=new TenantMemoryId("a",80001,1).encode(),bId=new TenantMemoryId("b",80001,1).encode();
        try(var workers=Executors.newSingleThreadExecutor()) {
            workers.submit(() -> memory.add(aId,List.of(new UserMessage("a-only")))).get();
            workers.submit(() -> memory.add(bId,List.of(new UserMessage("b-only")))).get();
            assertThat(workers.submit(() -> memory.get(aId)).get()).extracting(Message::getText).containsExactly("a-only");
            assertThat(workers.submit(TenantScope::current).get()).isNull();
        }
        assertThat(memory.get(bId)).extracting(Message::getText).containsExactly("b-only");
        assertThatThrownBy(() -> memory.get(new TenantMemoryId("a",80002,1).encode())).isInstanceOf(TenantException.class);
        try(var scope=TenantScope.open("b")) { assertThatThrownBy(() -> memory.get(aId)).isInstanceOf(TenantException.class); assertThat(TenantScope.current()).isEqualTo("b"); }
    }
    private NotificationPublishDTO notification(String request,String title) {
        return NotificationPublishDTO.builder().requestId(request).title(title).content("body").sourceType(NotificationSourceType.BUSINESS).build();
    }
    @Test void actualAfterCommitListenerUsesNewTransactionAndTenantQualifiedPush() throws Exception {
        String request="same-request-both-tenants";
        var tx=new TransactionTemplate(new JdbcTransactionManager(routing));
        for(String id:List.of("a","b"))try(var scope=TenantScope.open(id)) {
            tx.executeWithoutResult(status -> publisher.publish(notification(request,id)));
            tx.executeWithoutResult(status -> publisher.publish(notification(request,id)));
        }
        for(String id:List.of("a","b")) assertThat(db(id).queryForObject("SELECT COUNT(*) FROM eip_notification_publish WHERE request_id=?",Integer.class,request)).isEqualTo(1);
        verify(aSocket,times(1)).sendMessage(any()); verify(bSocket,times(1)).sendMessage(any());
        try(var scope=TenantScope.open("a")) {tx.executeWithoutResult(status -> {publisher.publish(notification("rolled-back","never"));status.setRollbackOnly();});}
        assertThat(db("a").queryForObject("SELECT COUNT(*) FROM eip_notification_publish WHERE request_id='rolled-back'",Integer.class)).isZero();
        assertThat(TenantScope.current()).isNull();
    }
    @Test void delayedListenerRestoresTenantBeforeOpeningTransaction() throws Exception {
        var event=new NotificationPublishEvent(this,notification("delayed","a"),"a");
        try(var workers=Executors.newSingleThreadExecutor()) {
            workers.submit(() -> listener.handle(event)).get();
            assertThat(workers.submit(TenantScope::current).get()).isNull();
        }
        assertThat(db("a").queryForObject("SELECT COUNT(*) FROM eip_notification_publish WHERE request_id='delayed'",Integer.class)).isEqualTo(1);
        assertThat(db("b").queryForObject("SELECT COUNT(*) FROM eip_notification_publish WHERE request_id='delayed'",Integer.class)).isZero();
        verify(aSocket).sendMessage(any());verify(bSocket,never()).sendMessage(any());
    }
    private ChatResponse response(String text) { return new ChatResponse(List.of(new Generation(new AssistantMessage(text)))); }
    private AiAssistantService assistant(String behavior) {
        ChatModel model=mock(ChatModel.class);
        when(model.getOptions()).thenReturn(org.springframework.ai.chat.prompt.ChatOptions.builder().build());
        when(model.call(any(Prompt.class))).thenAnswer(call -> response("{\"title\":\"tenant-title\"}"));
        when(model.stream(any(Prompt.class))).thenAnswer(call -> {
            if(behavior.equals("error"))return Flux.error(new IllegalStateException("model failure"));
            Flux<ChatResponse> first=Flux.just(response("answer-"+behavior)).publishOn(Schedulers.boundedElastic());
            return behavior.equals("never")?first.concatWith(Flux.never()):first;
        });
        var queries=mock(AiQueryTools.class);when(queries.all()).thenReturn(new Object[0]);
        var writes=mock(AiActionTool.class);when(writes.all()).thenReturn(new Object[0]);
        var actions=mock(AiActionService.class);when(actions.createPendingAction(anyList(),anyLong(),anyLong())).thenAnswer(call -> { assertThat(TenantScope.required()).isEqualTo("a");assertThat(new CurrentUserImpl().getUserId()).isEqualTo(80001L);return List.of();});
        return new AiAssistantServiceImpl(ChatClient.builder(model),memory,queries,writes,executor,TenantTestSupport.multiKeys(),messages,new AiConversationTitleService(ChatClient.builder(model),conversations),actions,new ObjectMapper());
    }
    private Flux<AiAssistantStreamResult> prepare(AiAssistantService assistant) {
        try(var scope=TenantScope.open("a")) {TenantContextTests.login("a",80001);return assistant.chatStream(new AiAssistantRequest(1L,"question"));}
        finally {SecurityContextHolder.clearContext();}
    }
    @Test void realStreamAdvisorsAndCallbacksWriteOnlyCapturedTenant() {
        var stream=prepare(assistant("normal"));
        var results=stream.collectList().block(Duration.ofSeconds(10));
        assertThat(results).noneMatch(result -> result.type()==AiStreamType.ERROR);
        assertThat(db("a").queryForObject("SELECT COUNT(*) FROM ai_message WHERE role='assistant'",Integer.class)).isEqualTo(1);
        assertThat(db("b").queryForObject("SELECT COUNT(*) FROM ai_message",Integer.class)).isZero();
        assertThat(db("a").queryForObject("SELECT title FROM ai_conversation WHERE id=1",String.class)).isEqualTo("tenant-title");
        assertThat(db("b").queryForObject("SELECT title FROM ai_conversation WHERE id=1",String.class)).isEqualTo("新对话");
        assertThat(db("a").queryForObject("SELECT COUNT(*) FROM SPRING_AI_CHAT_MEMORY",Integer.class)).isGreaterThan(0);
        assertThat(TenantScope.current()).isNull(); assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
    @Test void modelFailureAndCancellationDoNotRunSuccessCallbacks() {
        var failure=prepare(assistant("error")).collectList().block(Duration.ofSeconds(10));
        assertThat(failure).anyMatch(result -> result.type()==AiStreamType.ERROR);
        prepare(assistant("never")).take(1).blockLast(Duration.ofSeconds(10));
        assertThat(db("a").queryForObject("SELECT COUNT(*) FROM ai_message WHERE role='assistant'",Integer.class)).isZero();
        assertThat(db("b").queryForObject("SELECT COUNT(*) FROM ai_message",Integer.class)).isZero();
        assertThat(TenantScope.current()).isNull();
    }
    @Test void suspendingTenantTerminatesAnAlreadyRunningStream() {
        try {
            var result=prepare(assistant("never")).doOnNext(event -> {
                if(event.type()==AiStreamType.CONTENT)db("platform").update("UPDATE plt_tenant SET status='SUSPENDED' WHERE id='a'");
            }).collectList().block(Duration.ofSeconds(10));
            assertThat(result).anyMatch(event -> event.type()==AiStreamType.ERROR);
            assertThat(db("a").queryForObject("SELECT COUNT(*) FROM ai_message WHERE role='assistant'",Integer.class)).isZero();
        } finally {db("platform").update("UPDATE plt_tenant SET status='ACTIVE' WHERE id='a'");}
    }
    @Test void suspensionBeforeSubscriptionProducesErrorWithoutDeferredWrites() {
        var stream=prepare(assistant("normal"));
        try {
            db("platform").update("UPDATE plt_tenant SET status='SUSPENDED' WHERE id='a'");
            var result=stream.collectList().block(Duration.ofSeconds(10));
            assertThat(result).extracting(AiAssistantStreamResult::type).containsExactly(AiStreamType.ERROR);
            assertThat(db("a").queryForObject("SELECT COUNT(*) FROM ai_message WHERE role='assistant'",Integer.class)).isZero();
        } finally {db("platform").update("UPDATE plt_tenant SET status='ACTIVE' WHERE id='a'");}
    }
}
