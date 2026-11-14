package vn.edu.tvu.vinhlongtour.auth.service;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Chạy một lần mỗi khi backend khởi động: tạo tài khoản admin đầu tiên nếu CSDL chưa có.
 */
@Component
class AdminInitializer implements ApplicationRunner {

    private final AccountService accountService;

    AdminInitializer(AccountService accountService) {
        this.accountService = accountService;
    }

    @Override
    public void run(ApplicationArguments args) {
        accountService.createAdminIfMissing();
    }
}
