package vn.edu.tvu.vinhlongtour.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import vn.edu.tvu.vinhlongtour.dto.AuthDtos.*;
import vn.edu.tvu.vinhlongtour.dto.BookingDtos.BookingSummary;
import vn.edu.tvu.vinhlongtour.security.CurrentUser;
import vn.edu.tvu.vinhlongtour.service.AuthService;
import vn.edu.tvu.vinhlongtour.service.BookingService;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final BookingService bookingService;

    @PostMapping("/api/auth/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest r) {
        return authService.register(r);
    }

    @PostMapping("/api/auth/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest r) {
        return authService.login(r);
    }

    @PostMapping("/api/auth/google")
    public AuthResponse google(@Valid @RequestBody GoogleLoginRequest r) {
        return authService.loginGoogle(r);
    }

    /* ----- Khu vực khách hàng đã đăng nhập ----- */
    @GetMapping("/api/me")
    public UserInfo me() {
        return authService.me(CurrentUser.require().taiKhoanId());
    }

    @PutMapping("/api/me")
    public UserInfo update(@Valid @RequestBody UpdateProfileRequest r) {
        return authService.updateProfile(CurrentUser.require().taiKhoanId(), r);
    }

    @PutMapping("/api/me/mat-khau")
    public void changePassword(@Valid @RequestBody ChangePasswordRequest r) {
        authService.changePassword(CurrentUser.require().taiKhoanId(), r);
    }

    @GetMapping("/api/me/bookings")
    public List<BookingSummary> myBookings() {
        return bookingService.myBookings(CurrentUser.require().taiKhoanId());
    }
}
