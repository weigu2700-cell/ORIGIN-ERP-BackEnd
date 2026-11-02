package org.smart.erp.tenant;

import org.junit.jupiter.api.*;
import org.smart.erp.common.security.*;
import org.smart.erp.common.websocket.WebSocketSessionManager;
import org.smart.erp.system.Enum.UserStatus;
import org.smart.erp.system.entity.User;
import org.smart.erp.system.service.UserService;
import org.smart.erp.tenant.context.*;
import org.smart.erp.tenant.registry.TenantCatalog;
import org.springframework.mock.web.*;
import org.springframework.http.server.*;
import org.springframework.web.socket.*;
import java.time.Instant;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class TenantWebSocketTests {
    private TenantCatalog catalog;
    private WebSocketSessionManager sessions;
    private WebSocketAuthInterceptor handshake;
    private UserService users;
    private JwtUtil jwt;
    @BeforeEach void setup() {
        catalog=mock(TenantCatalog.class);
        when(catalog.byId(anyString())).thenAnswer(call -> TenantContextTests.descriptor(call.getArgument(0)));
        var resolver=TenantTestSupport.multiResolver(catalog);
        sessions=new WebSocketSessionManager(new TenantExecutor(TenantTestSupport.multiKeys(),resolver));
        users=mock(UserService.class);
        jwt=new JwtUtil(Base64.getEncoder().encodeToString("websocket-test-secret-at-least-32-bytes".getBytes()),60000);
        handshake=new WebSocketAuthInterceptor(jwt,resolver,users);
    }
    private WebSocketSession session(String id) {
        var session=mock(WebSocketSession.class);
        when(session.getId()).thenReturn(id); when(session.isOpen()).thenReturn(true);
        when(session.getAttributes()).thenReturn(new HashMap<>(Map.of("expiresAt",Instant.now().plusSeconds(60))));
        return session;
    }
    @Test void sameUserIdReceivesOnlyOwnEnterpriseMessages() throws Exception {
        var a=session("a"); var b=session("b");
        sessions.addSession("a",80001L,a); sessions.addSession("b",80001L,b);
        sessions.sendMessage("a",80001L,"a-only");
        verify(a).sendMessage(argThat(message -> message.getPayload().equals("a-only")));
        verify(b,never()).sendMessage(any());
        sessions.removeSession("a",80001L,a);
        sessions.sendMessage("b",80001L,"b-only");
        verify(b).sendMessage(argThat(message -> message.getPayload().equals("b-only")));
    }
    @Test void suspensionClosesIdleSessionsWithoutWaitingForPush() throws Exception {
        var a=session("a"); var b=session("b");
        sessions.addSession("a",80001L,a); sessions.addSession("b",80001L,b);
        when(catalog.byId("a")).thenThrow(new TenantException(403,"suspended"));
        sessions.reap();
        verify(a).close(CloseStatus.POLICY_VIOLATION); verify(b,never()).close(any());
    }
    @Test void expiredTokenSessionIsNeverSentMessages() throws Exception {
        var a=session("a"); a.getAttributes().put("expiresAt",Instant.now().minusSeconds(1));
        sessions.addSession("a",80001L,a); sessions.sendMessage("a",80001L,"expired");
        verify(a,never()).sendMessage(any()); verify(a).close(CloseStatus.POLICY_VIOLATION);
    }
    @Test void handshakeBindsVerifiedTenantAndLooksUpUserInThatScope() {
        var user=new User(); user.setStatus(UserStatus.NORMAL);
        when(users.detailUserById(80001L)).thenAnswer(call -> { assertThat(TenantScope.required()).isEqualTo("a"); return user; });
        var request=new MockHttpServletRequest("GET","/ws/notification");
        request.setQueryString("token="+jwt.generateToken(80001L,"a"));
        var attrs=new HashMap<String,Object>();
        assertThat(handshake.beforeHandshake(new ServletServerHttpRequest(request), new ServletServerHttpResponse(new MockHttpServletResponse()), mock(WebSocketHandler.class),attrs)).isTrue();
        assertThat(attrs.get("tenantId")).isEqualTo("a"); assertThat(attrs.get("expiresAt")).isInstanceOf(Instant.class);
        assertThat(TenantScope.current()).isNull();
    }
    @Test void conflictingHeaderAndLegacyTokenCannotEstablishMultiTenantSocket() {
        for (boolean legacy : List.of(false,true)) {
            var request=new MockHttpServletRequest("GET","/ws/notification");
            request.setQueryString("token="+(legacy?jwt.generateToken(80001L):jwt.generateToken(80001L,"a")));
            if(!legacy) request.addHeader("X-Tenant-ID","b");
            var response=new MockHttpServletResponse();
            assertThat(handshake.beforeHandshake(new ServletServerHttpRequest(request),new ServletServerHttpResponse(response),mock(WebSocketHandler.class),new HashMap<>())).isFalse();
            assertThat(response.getStatus()).isEqualTo(legacy?401:403);
        }
        verifyNoInteractions(users);
    }
}
