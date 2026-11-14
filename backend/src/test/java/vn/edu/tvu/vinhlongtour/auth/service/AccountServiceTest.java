package vn.edu.tvu.vinhlongtour.auth.service;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import vn.edu.tvu.vinhlongtour.auth.domain.Role;
import vn.edu.tvu.vinhlongtour.auth.repository.AccountRepository;
import vn.edu.tvu.vinhlongtour.auth.security.JwtService;
import vn.edu.tvu.vinhlongtour.common.AppProperties;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Kiểm tra bước tạo admin lúc khởi động: không được làm backend dừng khi email admin
 * đã bị một khách hàng đăng ký trước.
 */
class AccountServiceTest {

    private final AccountRepository accounts = mock(AccountRepository.class);
    private final AppProperties props = new AppProperties(null,
            new AppProperties.Admin("Admin@VinhLong.vn", "Admin@12345"), null, null, null, null);
    private final AccountService service = new AccountService(accounts, mock(PasswordEncoder.class),
            mock(JwtService.class), mock(GoogleTokenVerifier.class), props);

    @Test
    void adminIsNotCreatedWhenEmailAlreadyBelongsToCustomer() {
        when(accounts.existsByRole(Role.ADMIN)).thenReturn(false);
        when(accounts.existsByEmail("admin@vinhlong.vn")).thenReturn(true);

        service.createAdminIfMissing();

        verify(accounts, never()).save(any());
    }

    @Test
    void adminIsCreatedWhenNoneExists() {
        when(accounts.existsByRole(Role.ADMIN)).thenReturn(false);
        when(accounts.existsByEmail("admin@vinhlong.vn")).thenReturn(false);

        service.createAdminIfMissing();

        verify(accounts).save(any());
    }
}
