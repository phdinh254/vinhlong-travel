package vn.edu.tvu.vinhlongtour.auth.domain;

/**
 * Trạng thái tài khoản. Tài khoản {@code LOCKED} không đăng nhập được và
 * token đã cấp trước đó cũng bị từ chối ngay lập tức.
 */
public enum AccountStatus {
    ACTIVE,
    LOCKED
}
