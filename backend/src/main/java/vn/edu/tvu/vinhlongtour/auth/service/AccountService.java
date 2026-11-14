package vn.edu.tvu.vinhlongtour.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.tvu.vinhlongtour.auth.domain.Account;
import vn.edu.tvu.vinhlongtour.auth.domain.AccountStatus;
import vn.edu.tvu.vinhlongtour.auth.domain.Role;
import vn.edu.tvu.vinhlongtour.auth.repository.AccountRepository;
import vn.edu.tvu.vinhlongtour.auth.security.JwtService;
import vn.edu.tvu.vinhlongtour.auth.service.GoogleTokenVerifier.GoogleUser;
import vn.edu.tvu.vinhlongtour.auth.web.AuthDtos.AccountResponse;
import vn.edu.tvu.vinhlongtour.auth.web.AuthDtos.AuthResponse;
import vn.edu.tvu.vinhlongtour.auth.web.AuthDtos.ChangePasswordRequest;
import vn.edu.tvu.vinhlongtour.auth.web.AuthDtos.LoginRequest;
import vn.edu.tvu.vinhlongtour.auth.web.AuthDtos.RegisterRequest;
import vn.edu.tvu.vinhlongtour.auth.web.AuthDtos.UpdateProfileRequest;
import vn.edu.tvu.vinhlongtour.common.ApiException;
import vn.edu.tvu.vinhlongtour.common.AppProperties;
import vn.edu.tvu.vinhlongtour.common.PageResponse;

import java.nio.charset.StandardCharsets;

/**
 * Nghiệp vụ tài khoản: đăng ký, đăng nhập (mật khẩu hoặc Google), hồ sơ cá nhân,
 * đổi mật khẩu, và admin khóa/mở khóa tài khoản.
 */
@Service
@Transactional
public class AccountService {

    private static final Logger log = LoggerFactory.getLogger(AccountService.class);
    private static final int BCRYPT_MAX_BYTES = 72;

    private final AccountRepository accounts;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final GoogleTokenVerifier googleVerifier;
    private final AppProperties props;

    public AccountService(AccountRepository accounts, PasswordEncoder passwordEncoder, JwtService jwtService,
                          GoogleTokenVerifier googleVerifier, AppProperties props) {
        this.accounts = accounts;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.googleVerifier = googleVerifier;
        this.props = props;
    }

    public AuthResponse register(RegisterRequest r) {
        String email = Account.normalizeEmail(r.email());
        if (accounts.existsByEmail(email)) {
            throw ApiException.conflict("EMAIL_TAKEN", "Email đã được sử dụng");
        }
        Account account = Account.withPassword(email, encode(r.password()), r.fullName(), Role.CUSTOMER);
        account.updateProfile(r.fullName(), r.phone());
        return authResponse(accounts.save(account));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest r) {
        // Sai email hay sai mật khẩu đều báo chung một lỗi để không lộ email nào đã đăng ký
        Account account = accounts.findByEmail(Account.normalizeEmail(r.email()))
                .filter(a -> a.hasPassword() && passwordEncoder.matches(r.password(), a.getPasswordHash()))
                .orElseThrow(() -> ApiException.unauthorized("INVALID_CREDENTIALS", "Email hoặc mật khẩu không đúng"));
        return authResponse(ensureActive(account));
    }

    /** Tìm theo mã Google → nếu chưa có thì theo email (liên kết vào tài khoản sẵn có) → nếu vẫn chưa có thì tạo mới. */
    public AuthResponse loginWithGoogle(String credential) {
        GoogleUser g = googleVerifier.verify(credential);
        Account account = accounts.findByGoogleSub(g.sub())
                .or(() -> accounts.findByEmail(Account.normalizeEmail(g.email())))
                .orElseGet(() -> accounts.save(Account.fromGoogle(g.email(), g.name() != null ? g.name() : g.email(), g.sub(), g.picture())));
        account.linkGoogle(g.sub(), g.picture());
        return authResponse(ensureActive(account));
    }

    @Transactional(readOnly = true)
    public AccountResponse me(Long accountId) {
        return AccountResponse.from(find(accountId));
    }

    public AccountResponse updateProfile(Long accountId, UpdateProfileRequest r) {
        Account account = find(accountId);
        account.updateProfile(r.fullName(), r.phone());
        return AccountResponse.from(account);
    }

    public void changePassword(Long accountId, ChangePasswordRequest r) {
        Account account = find(accountId);
        if (account.hasPassword() && (r.currentPassword() == null || !passwordEncoder.matches(r.currentPassword(), account.getPasswordHash()))) {
            throw ApiException.badRequest("WRONG_PASSWORD", "Mật khẩu hiện tại không đúng");
        }
        account.changePasswordHash(encode(r.newPassword()));
    }

    @Transactional(readOnly = true)
    public PageResponse<AccountResponse> search(String q, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, 100), Sort.by(Sort.Direction.DESC, "createdAt"));
        var result = q == null || q.isBlank()
                ? accounts.findAll(pageable)
                : accounts.findByEmailContainingIgnoreCaseOrFullNameContainingIgnoreCase(q.trim(), q.trim(), pageable);
        return PageResponse.of(result.map(AccountResponse::from));
    }

    public AccountResponse changeStatus(Long adminId, Long accountId, AccountStatus status) {
        if (adminId.equals(accountId) && status == AccountStatus.LOCKED) {
            throw ApiException.badRequest("CANNOT_LOCK_SELF", "Không thể tự khóa tài khoản của chính mình");
        }
        Account account = find(accountId);
        if (status == AccountStatus.LOCKED) {
            account.lock();
        } else {
            account.unlock();
        }
        return AccountResponse.from(account);
    }

    /** Gọi lúc khởi động: tạo admin đầu tiên từ ADMIN_EMAIL / ADMIN_PASSWORD nếu CSDL chưa có admin. */
    public void createAdminIfMissing() {
        if (accounts.existsByRole(Role.ADMIN)) {
            return;
        }
        AppProperties.Admin admin = props.admin();
        if (admin.password() == null || admin.password().isBlank()) {
            log.warn("Chưa có tài khoản admin. Đặt biến môi trường ADMIN_PASSWORD (và ADMIN_EMAIL) rồi khởi động lại để tạo.");
            return;
        }
        accounts.save(Account.withPassword(admin.email(), encode(admin.password()), "Quản trị viên", Role.ADMIN));
        log.info("Đã tạo tài khoản admin {}", Account.normalizeEmail(admin.email()));
    }

    private String encode(String rawPassword) {
        // BCrypt chỉ dùng 72 byte đầu; chữ có dấu chiếm 2-3 byte nên phải kiểm tra theo byte
        if (rawPassword.getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_BYTES) {
            throw ApiException.badRequest("PASSWORD_TOO_LONG", "Mật khẩu quá dài");
        }
        return passwordEncoder.encode(rawPassword);
    }

    private Account find(Long accountId) {
        return accounts.findById(accountId)
                .orElseThrow(() -> ApiException.notFound("ACCOUNT_NOT_FOUND", "Không tìm thấy tài khoản"));
    }

    private static Account ensureActive(Account account) {
        if (account.isLocked()) {
            throw ApiException.forbidden("ACCOUNT_LOCKED", "Tài khoản đã bị khóa");
        }
        return account;
    }

    private AuthResponse authResponse(Account account) {
        JwtService.IssuedToken token = jwtService.issue(account);
        return new AuthResponse(token.value(), token.expiresAt(), AccountResponse.from(account));
    }
}
