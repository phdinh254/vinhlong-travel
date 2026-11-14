package vn.edu.tvu.vinhlongtour.auth.web;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.tvu.vinhlongtour.auth.service.AccountService;
import vn.edu.tvu.vinhlongtour.auth.web.AuthDtos.AccountResponse;
import vn.edu.tvu.vinhlongtour.auth.web.AuthDtos.UpdateStatusRequest;
import vn.edu.tvu.vinhlongtour.common.PageResponse;

/**
 * API quản trị tài khoản (chỉ ADMIN): tìm tài khoản theo email/họ tên, khóa và mở khóa.
 */
@RestController
@RequestMapping("/api/v1/admin/accounts")
public class AdminAccountController {

    private final AccountService accountService;

    public AdminAccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping
    public PageResponse<AccountResponse> search(@RequestParam(required = false) String q,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "20") int size) {
        return accountService.search(q, page, size);
    }

    @PatchMapping("/{id}/status")
    public AccountResponse changeStatus(Authentication auth, @PathVariable Long id, @Valid @RequestBody UpdateStatusRequest r) {
        return accountService.changeStatus(Long.valueOf(auth.getName()), id, r.status());
    }
}
