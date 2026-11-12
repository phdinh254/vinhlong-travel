package vn.edu.tvu.vinhlongtour.security;

/** Thông tin người dùng đã xác thực, đặt vào SecurityContext */
public record AuthUser(Integer taiKhoanId, String email, String vaiTro) {
    public boolean isAdmin() {
        return "ADMIN".equals(vaiTro);
    }
}
