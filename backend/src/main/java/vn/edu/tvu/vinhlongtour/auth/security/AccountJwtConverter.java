package vn.edu.tvu.vinhlongtour.auth.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import vn.edu.tvu.vinhlongtour.auth.domain.Account;
import vn.edu.tvu.vinhlongtour.auth.repository.AccountRepository;

import java.util.List;

/**
 * Đổi token hợp lệ thành người dùng đang đăng nhập. Đọc lại tài khoản từ CSDL ở mỗi request:
 * tài khoản đã bị khóa hoặc bị xóa thì từ chối ngay dù token còn hạn,
 * và vai trò luôn lấy theo CSDL chứ không theo token.
 * Tên của người dùng ({@code Authentication#getName()}) là mã tài khoản.
 */
@Component
public class AccountJwtConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final AccountRepository accounts;

    public AccountJwtConverter(AccountRepository accounts) {
        this.accounts = accounts;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Account account = accounts.findById(Long.valueOf(jwt.getSubject()))
                .orElseThrow(() -> new BadCredentialsException("Tài khoản không còn tồn tại"));
        if (account.isLocked()) {
            throw new LockedException("Tài khoản đã bị khóa");
        }
        return new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_" + account.getRole().name())), jwt.getSubject());
    }
}
