package vn.edu.tvu.vinhlongtour.auth.web;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.tvu.vinhlongtour.auth.service.AccountService;
import vn.edu.tvu.vinhlongtour.auth.web.AuthDtos.AccountResponse;
import vn.edu.tvu.vinhlongtour.auth.web.AuthDtos.ChangePasswordRequest;
import vn.edu.tvu.vinhlongtour.auth.web.AuthDtos.UpdateProfileRequest;

/**
 * API cho người đang đăng nhập tự xem và sửa hồ sơ, đổi mật khẩu.
 * Mã tài khoản lấy từ token ({@code Authentication#getName()}), không nhận từ client.
 */
@RestController
@RequestMapping("/api/v1/me")
public class MeController {

    private final AccountService accountService;

    public MeController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping
    public AccountResponse me(Authentication auth) {
        return accountService.me(Long.valueOf(auth.getName()));
    }

    @PutMapping
    public AccountResponse update(Authentication auth, @Valid @RequestBody UpdateProfileRequest r) {
        return accountService.updateProfile(Long.valueOf(auth.getName()), r);
    }

    @PutMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(Authentication auth, @Valid @RequestBody ChangePasswordRequest r) {
        accountService.changePassword(Long.valueOf(auth.getName()), r);
    }
}
