package vn.edu.tvu.vinhlongtour.auth.domain;

/**
 * Vai trò của tài khoản: quản trị viên hoặc khách hàng.
 * Spring Security dùng dưới dạng quyền {@code ROLE_ADMIN}, {@code ROLE_CUSTOMER}.
 */
public enum Role {
    ADMIN,
    CUSTOMER
}
