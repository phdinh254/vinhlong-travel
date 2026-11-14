package vn.edu.tvu.vinhlongtour.auth.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Lấy mã tài khoản đang đăng nhập ở các API mà khách vãng lai cũng gọi được (ví dụ đặt tour).
 */
public final class CurrentAccount {

    private CurrentAccount() {
    }

    /** Mã tài khoản nếu request có token hợp lệ; null nếu là khách vãng lai. */
    public static Long idOrNull(Authentication auth) {
        return auth instanceof JwtAuthenticationToken ? Long.valueOf(auth.getName()) : null;
    }
}
