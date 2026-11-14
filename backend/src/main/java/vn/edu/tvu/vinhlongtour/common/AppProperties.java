package vn.edu.tvu.vinhlongtour.common;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

/**
 * Các cấu hình riêng của ứng dụng (nhóm {@code app:} trong application.yml),
 * phần lớn đọc từ biến môi trường để không phải ghi bí mật vào mã nguồn.
 */
@ConfigurationProperties("app")
public record AppProperties(Jwt jwt, Admin admin, Google google, Cors cors) {

    /** Khóa ký token đăng nhập và thời gian sống của token. */
    public record Jwt(String secret, Duration ttl) {}

    /** Tài khoản admin được tạo lần đầu khi CSDL chưa có admin nào. */
    public record Admin(String email, String password) {}

    /** Client ID của ứng dụng trên Google Cloud, dùng để kiểm tra token đăng nhập Google. */
    public record Google(String clientId) {}

    /** Các địa chỉ frontend được phép gọi API từ trình duyệt. */
    public record Cors(List<String> allowedOrigins) {}
}
