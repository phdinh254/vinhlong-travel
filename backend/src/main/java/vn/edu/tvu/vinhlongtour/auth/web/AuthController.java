package vn.edu.tvu.vinhlongtour.auth.web;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.tvu.vinhlongtour.auth.service.AccountService;
import vn.edu.tvu.vinhlongtour.auth.web.AuthDtos.AuthResponse;
import vn.edu.tvu.vinhlongtour.auth.web.AuthDtos.GoogleLoginRequest;
import vn.edu.tvu.vinhlongtour.auth.web.AuthDtos.LoginRequest;
import vn.edu.tvu.vinhlongtour.auth.web.AuthDtos.RegisterRequest;

/**
 * API công khai để lấy token đăng nhập: đăng ký, đăng nhập bằng mật khẩu, đăng nhập bằng Google.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AccountService accountService;

    public AuthController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest r) {
        return accountService.register(r);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest r) {
        return accountService.login(r);
    }

    @PostMapping("/google")
    public AuthResponse google(@Valid @RequestBody GoogleLoginRequest r) {
        return accountService.loginWithGoogle(r.credential());
    }
}
