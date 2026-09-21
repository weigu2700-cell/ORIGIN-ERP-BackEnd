package org.smart.erp.common.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CurrentUserImplTest {

    private final CurrentUserImpl currentUser = new CurrentUserImpl();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void rejectsMissingAuthenticationWith401BusinessException() {
        var exception = assertThrows(
                org.smart.erp.common.exception.BusinessException.class,
                currentUser::getUserId
        );

        assertEquals(401, exception.getCode());
    }

    @Test
    void returnsIdForAuthenticatedLoginUser() {
        var loginUser = new LoginUser(42L, "tester");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, java.util.List.of())
        );

        assertEquals(42L, currentUser.getUserId());
    }

    @Test
    void rejectsAuthenticatedNonLoginPrincipalWith401BusinessException() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("anonymousUser", null, java.util.List.of())
        );

        var exception = assertThrows(
                org.smart.erp.common.exception.BusinessException.class,
                currentUser::getUsername
        );

        assertEquals(401, exception.getCode());
    }
}
