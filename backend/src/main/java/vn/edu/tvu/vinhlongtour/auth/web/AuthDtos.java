package vn.edu.tvu.vinhlongtour.auth.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import vn.edu.tvu.vinhlongtour.auth.domain.Account;
import vn.edu.tvu.vinhlongtour.auth.domain.AccountStatus;
import vn.edu.tvu.vinhlongtour.auth.domain.Role;

import java.time.Instant;

/**
 * Dữ liệu vào/ra của các API tài khoản. Ràng buộc {@code @NotBlank}, {@code @Email}...
 * được kiểm tra trước khi vào service; sai thì trả lỗi {@code VALIDATION_FAILED} kèm tên trường.
 */
public final class AuthDtos {

    private static final String PHONE = "^[0-9+]{9,15}$";

    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank @Email @Size(max = 150) String email,
            @NotBlank @Size(min = 8, max = 64) String password,
            @NotBlank @Size(max = 150) String fullName,
            @Pattern(regexp = PHONE, message = "số điện thoại không hợp lệ") String phone) {}

    public record LoginRequest(@NotBlank String email, @NotBlank String password) {}

    public record GoogleLoginRequest(@NotBlank String credential) {}

    public record UpdateProfileRequest(
            @NotBlank @Size(max = 150) String fullName,
            @Pattern(regexp = PHONE, message = "số điện thoại không hợp lệ") String phone) {}

    /** {@code currentPassword} bỏ trống được khi tài khoản chỉ đăng nhập Google, chưa có mật khẩu. */
    public record ChangePasswordRequest(String currentPassword, @NotBlank @Size(min = 8, max = 64) String newPassword) {}

    public record UpdateStatusRequest(@NotNull AccountStatus status) {}

    public record AccountResponse(Long id, String email, String fullName, String phone, String avatarUrl,
                                  Role role, AccountStatus status, boolean hasPassword, Instant createdAt) {

        public static AccountResponse from(Account a) {
            return new AccountResponse(a.getId(), a.getEmail(), a.getFullName(), a.getPhone(), a.getAvatarUrl(),
                    a.getRole(), a.getStatus(), a.hasPassword(), a.getCreatedAt());
        }
    }

    public record AuthResponse(String token, Instant expiresAt, AccountResponse account) {}
}
