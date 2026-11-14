package vn.edu.tvu.vinhlongtour.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Locale;

/**
 * Tài khoản đăng nhập của khách hàng hoặc quản trị viên.
 * Một tài khoản đăng nhập bằng email + mật khẩu, bằng Google, hoặc cả hai (khi đã liên kết).
 * Các trường chỉ đổi qua phương thức nghiệp vụ (khóa, đổi mật khẩu, liên kết Google...),
 * không có setter để nơi khác không sửa tùy tiện.
 */
@Entity
@Table(name = "accounts")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(length = 100)
    private String passwordHash;

    @Column(nullable = false, length = 150)
    private String fullName;

    @Column(length = 20)
    private String phone;

    @Column(length = 500)
    private String avatarUrl;

    @Column(unique = true, length = 64)
    private String googleSub;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountStatus status;

    @Column(nullable = false)
    private Instant createdAt;

    protected Account() {
        // JPA cần constructor rỗng
    }

    private Account(String email, String fullName, Role role) {
        this.email = normalizeEmail(email);
        this.fullName = fullName.trim();
        this.role = role;
        this.status = AccountStatus.ACTIVE;
        this.createdAt = Instant.now();
    }

    /** Tạo tài khoản đăng nhập bằng email và mật khẩu (đã băm). */
    public static Account withPassword(String email, String passwordHash, String fullName, Role role) {
        Account a = new Account(email, fullName, role);
        a.passwordHash = passwordHash;
        return a;
    }

    /** Tạo tài khoản khách hàng lần đầu đăng nhập bằng Google, chưa có mật khẩu. */
    public static Account fromGoogle(String email, String fullName, String googleSub, String avatarUrl) {
        Account a = new Account(email, fullName, Role.CUSTOMER);
        a.googleSub = googleSub;
        a.avatarUrl = avatarUrl;
        return a;
    }

    /** Email so sánh không phân biệt hoa thường nên luôn lưu chữ thường, bỏ khoảng trắng thừa. */
    public static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    public void updateProfile(String fullName, String phone) {
        this.fullName = fullName.trim();
        this.phone = phone == null || phone.isBlank() ? null : phone.trim();
    }

    public void changePasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    /** Gắn tài khoản Google vào tài khoản này; giữ ảnh đại diện cũ nếu đã có. */
    public void linkGoogle(String googleSub, String avatarUrl) {
        this.googleSub = googleSub;
        if (this.avatarUrl == null) {
            this.avatarUrl = avatarUrl;
        }
    }

    public void lock() {
        this.status = AccountStatus.LOCKED;
    }

    public void unlock() {
        this.status = AccountStatus.ACTIVE;
    }

    public boolean isLocked() {
        return status == AccountStatus.LOCKED;
    }

    /** Tài khoản chỉ đăng nhập bằng Google thì chưa có mật khẩu. */
    public boolean hasPassword() {
        return passwordHash != null;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getFullName() {
        return fullName;
    }

    public String getPhone() {
        return phone;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public String getGoogleSub() {
        return googleSub;
    }

    public Role getRole() {
        return role;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
