package org.smart.erp.tenant;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.smart.erp.common.config.RedisConfig;
import org.smart.erp.common.sequence.BusinessNoGenerator;
import org.smart.erp.common.utils.redis.*;
import org.smart.erp.system.cache.*;
import org.smart.erp.system.vo.*;
import org.smart.erp.tenant.context.*;
import org.smart.erp.ai.enums.*;
import org.smart.erp.ai.model.AiPendingAction;
import org.smart.erp.ai.service.AiConversationService;
import org.smart.erp.ai.service.impl.AiActionServiceImpl;
import org.smart.erp.purchase.entity.PurchaseOrder;
import org.smart.erp.purchase.service.PurchaseOrderService;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@EnabledIfEnvironmentVariable(named="TENANT_TEST_REDIS_PORT",matches="[0-9]+")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TenantRedisIsolationTests {
    private final String prefix="redis_verify_"+UUID.randomUUID().toString().substring(0,8);
    private String a=prefix+"_a",b=prefix+"_b";
    private LettuceConnectionFactory connection;
    private StringRedisTemplate raw, strings;
    private OperationString values;
    private OperationSet sets;
    private RedissonClient redisson;
    @BeforeAll void setup() {
        int port=Integer.parseInt(System.getenv("TENANT_TEST_REDIS_PORT"));
        connection=new LettuceConnectionFactory("127.0.0.1",port); connection.afterPropertiesSet(); connection.start();
        var config=new RedisConfig(); var keys=TenantTestSupport.multiKeys();
        var template=config.redisTemplate(connection,new ObjectMapper().registerModule(new JavaTimeModule()),keys);
        values=new OperationString(template); sets=new OperationSet(template); strings=config.stringRedisTemplate(connection,keys);
        raw=new StringRedisTemplate(connection);
        var rc=new Config(); rc.useSingleServer().setAddress("redis://127.0.0.1:"+port).setConnectionMinimumIdleSize(1).setConnectionPoolSize(2);
        redisson=Redisson.create(rc);
    }
    @AfterAll void cleanup() {
        if(raw!=null) { var keys=raw.keys("erp:t:"+prefix+"*"); if(keys!=null&&!keys.isEmpty()) raw.delete(keys); }
        if(redisson!=null)redisson.shutdown(); if(connection!=null)connection.destroy();
    }
    @Test void permissionAndMenuInvalidationAffectsOnlyCurrentTenantIncludingIndices() {
        var permissions=new PermissionsRedis(values,sets); var menus=new MenuRedis(values,sets);
        for(String id:List.of(a,b))try(var scope=TenantScope.open(id)) {
            var permission=new PermissionCacheVo(); permission.setCode(id);
            permissions.activePermissionsCache(80001L,Set.of(permission));
            var menu=new MenuTreeVo(); menu.setName(id); menus.activeMenuCache(80001L,List.of(menu));
        }
        try(var scope=TenantScope.open(a)) { permissions.evictAllPermissionsCache(); menus.evictAllMenuCache(); }
        try(var scope=TenantScope.open(b)) {
            assertThat(permissions.getPermissionsCache(80001L)).extracting(PermissionCacheVo::getCode).containsExactly(b);
            assertThat(menus.getMenuCache(80001L)).extracting(MenuTreeVo::getName).containsExactly(b);
        }
        try(var scope=TenantScope.open(a)) { assertThat(permissions.getPermissionsCache(80001L)).isNull(); assertThat(menus.getMenuCache(80001L)).isNull(); }
    }
    @Test void sequencesAreIndependentAndMissingScopeNeverUsesGlobalKey() {
        var generator=new BusinessNoGenerator(strings); String first;
        try(var scope=TenantScope.open(a)) { first=generator.generateNo("erp:test:sequence:","TEST"); }
        try(var scope=TenantScope.open(b)) { assertThat(generator.generateNo("erp:test:sequence:","TEST")).isEqualTo(first); }
        assertThatThrownBy(generator::generateOrderNo).isInstanceOf(TenantException.class);
    }
    @Test void redissonLocksDoNotBlockOtherTenantWithSameBusinessId() throws Exception {
        var keys=TenantTestSupport.multiKeys(); String aKey;
        try(var scope=TenantScope.open(a)) { aKey=keys.key("erp:test:lock:1"); }
        var lock=redisson.getLock(aKey); lock.lock();
        try(var workers=Executors.newSingleThreadExecutor()) {
            assertThat(workers.submit(() -> { try(var scope=TenantScope.open(b)) {
                var other=redisson.getLock(keys.key("erp:test:lock:1")); boolean acquired=other.tryLock(100,TimeUnit.MILLISECONDS);
                if(acquired)other.unlock(); return acquired;
            }}).get()).isTrue();
            assertThat(workers.submit(() -> { try(var scope=TenantScope.open(a)) { return redisson.getLock(keys.key("erp:test:lock:1")).tryLock(100,TimeUnit.MILLISECONDS); }}).get()).isFalse();
        } finally { lock.unlock(); }
    }
    @Test void sameActionTokenAndUserCannotCrossTenantAndLockOwnerIsCheckedOnRelease() {
        String token="abcdef0123456789abcdef0123456789";
        String key="erp:ai:action:"+token,lock="erp:ai:action:executing:"+token;
        var orders=mock(PurchaseOrderService.class);
        var order=new PurchaseOrder();order.setId(11L);order.setPurchaseOrderNo("PO-1");order.setVersion(3);
        when(orders.getById(11L)).thenReturn(order);
        var service=new AiActionServiceImpl(orders,values,TenantTestSupport.multiKeys(),mock(AiConversationService.class));
        for(String id:List.of(a,b))try(var scope=TenantScope.open(id)) {
            var now=LocalDateTime.now(); values.set(key,new AiPendingAction(token,id,80001L,1L,AiActionType.APPROVE_PURCHASE_ORDER,11L,"PO-1",3,"title","body",AiActionStatus.PENDING,now,now.plusMinutes(10)),Duration.ofMinutes(10));
        }
        doAnswer(call -> { values.delete(lock); values.setIfAbsent(lock,"replacement-owner",Duration.ofMinutes(1)); return null; }).when(orders).approvePurchaseOrder(11L);
        try(var scope=TenantScope.open(a)) {
            service.executePendingAction(token,80001L);
            assertThat(values.<String>get(lock)).isEqualTo("replacement-owner"); assertThat(values.<Object>get(key)).isNull();
        }
        try(var scope=TenantScope.open(b)) { assertThat(values.<AiPendingAction>get(key).tenantId()).isEqualTo(b); }
        verify(orders,times(1)).approvePurchaseOrder(11L);
    }
    @Test void forgedActionValueFromOtherTenantIsRejectedBeforeDomainWrite() {
        String token="0123456789abcdef0123456789abcdef"; var orders=mock(PurchaseOrderService.class);
        var service=new AiActionServiceImpl(orders,values,TenantTestSupport.multiKeys(),mock(AiConversationService.class));
        try(var scope=TenantScope.open(a)) {
            var now=LocalDateTime.now();
            values.set("erp:ai:action:"+token,new AiPendingAction(token,b,80001L,1L,AiActionType.APPROVE_PURCHASE_ORDER,11L,"PO-1",3,"title","body",AiActionStatus.PENDING,now,now.plusMinutes(10)),Duration.ofMinutes(10));
            assertThatThrownBy(() -> service.executePendingAction(token,80001L)).hasMessageContaining("不属于当前企业");
        }
        verifyNoInteractions(orders);
    }
}
