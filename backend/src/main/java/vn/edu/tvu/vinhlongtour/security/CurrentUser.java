package vn.edu.tvu.vinhlongtour.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import vn.edu.tvu.vinhlongtour.exception.ApiException;

import java.util.Optional;

public final class CurrentUser {
    private CurrentUser() {}

    public static Optional<AuthUser> get() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a != null && a.getPrincipal() instanceof AuthUser u) return Optional.of(u);
        return Optional.empty();
    }

    public static AuthUser require() {
        return get().orElseThrow(() -> ApiException.unauthorized("Vui lòng đăng nhập"));
    }
}
