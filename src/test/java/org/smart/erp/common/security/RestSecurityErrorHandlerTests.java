package org.smart.erp.common.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.smart.erp.common.result.Result;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

import static org.assertj.core.api.Assertions.assertThat;

class RestSecurityErrorHandlerTests {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void authenticationFailureReturnsHttp401Result() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        new RestAuthenticationEntryPoint(objectMapper).commence(
                new MockHttpServletRequest(),
                response,
                new BadCredentialsException("invalid token")
        );

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).startsWith("application/json");
        assertResult(response, 401, "未认证或登录已失效");
    }

    @Test
    void filterAuthorizationFailureReturnsHttp403Result() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        new RestAccessDeniedHandler(objectMapper).handle(
                new MockHttpServletRequest(),
                response,
                new AccessDeniedException("denied")
        );

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentType()).startsWith("application/json");
        assertResult(response, 403, "无此操作权限");
    }

    @Test
    void methodAuthorizationFailureReturnsHttp403Result() {
        ResponseEntity<Result<Void>> response = new MethodSecurityExceptionHandler()
                .handleAccessDenied(new AccessDeniedException("denied"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(403);
        assertThat(response.getBody().getMsg()).isEqualTo("无此操作权限");
        assertThat(response.getBody().getData()).isNull();
    }

    private void assertResult(MockHttpServletResponse response, int code, String message) throws Exception {
        JsonNode result = objectMapper.readTree(response.getContentAsByteArray());
        assertThat(result.path("code").asInt()).isEqualTo(code);
        assertThat(result.path("msg").asText()).isEqualTo(message);
        assertThat(result.path("data").isNull()).isTrue();
    }
}
