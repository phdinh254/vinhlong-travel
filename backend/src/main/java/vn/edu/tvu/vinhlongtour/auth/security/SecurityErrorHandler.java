package vn.edu.tvu.vinhlongtour.auth.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Trả lỗi đăng nhập/phân quyền theo đúng dạng chung {@code {code, message, fields}}.
 * Cần riêng class này vì các lỗi này xảy ra trong bộ lọc bảo mật, trước khi tới controller,
 * nên {@code GlobalExceptionHandler} không bắt được.
 */
@Component
public class SecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException ex) throws IOException {
        if (ex instanceof LockedException) {
            write(response, HttpStatus.FORBIDDEN, "ACCOUNT_LOCKED", "Tài khoản đã bị khóa");
        } else {
            write(response, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Chưa đăng nhập hoặc phiên đăng nhập đã hết hạn");
        }
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex) throws IOException {
        write(response, HttpStatus.FORBIDDEN, "FORBIDDEN", "Bạn không có quyền thực hiện thao tác này");
    }

    // Các câu thông báo là hằng số do mình viết, không chứa ký tự cần thoát trong JSON
    private static void write(HttpServletResponse response, HttpStatus status, String code, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"code\":\"" + code + "\",\"message\":\"" + message + "\",\"fields\":{}}");
    }
}
