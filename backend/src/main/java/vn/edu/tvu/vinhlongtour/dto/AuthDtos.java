package vn.edu.tvu.vinhlongtour.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDtos {
    private AuthDtos() {}

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String matKhau) {}

    public record RegisterRequest(
            @NotBlank @Size(max = 150) String hoTen,
            @NotBlank @Email String email,
            @NotBlank @Size(min = 6, max = 64) String matKhau,
            @Size(max = 20) String soDienThoai) {}

    /** credential = Google ID token nhận từ Google Identity Services ở frontend */
    public record GoogleLoginRequest(@NotBlank String credential) {}

    public record UserInfo(Integer taiKhoanId, String email, String hoTen, String anhDaiDien,
                           String vaiTro, String nhaCungCap, String soDienThoai, String diaChi) {}

    public record AuthResponse(String token, UserInfo user) {}

    public record UpdateProfileRequest(@NotBlank @Size(max = 150) String hoTen,
                                       @Size(max = 20) String soDienThoai,
                                       @Size(max = 300) String diaChi) {}

    public record ChangePasswordRequest(String matKhauCu, @NotBlank @Size(min = 6, max = 64) String matKhauMoi) {}
}
