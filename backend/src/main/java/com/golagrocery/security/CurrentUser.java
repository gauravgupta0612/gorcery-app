package com.golagrocery.security;

import com.golagrocery.common.ApiException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class CurrentUser {
    private CurrentUser() {}

    public static AuthUser get() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null || !(a.getPrincipal() instanceof AuthUser u)) {
            throw ApiException.unauthorized("Not authenticated");
        }
        return u;
    }

    public static Long id() { return get().id(); }
}
