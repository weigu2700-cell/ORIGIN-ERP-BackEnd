package org.smart.erp.common.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserImpl implements CurrentUser {


    @Override
    public Long getUserId() {
        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();

        return loginUser.getUserId();
    }

    @Override
    public String getUsername() {
        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();
        return authentication.getName();
    }


}
