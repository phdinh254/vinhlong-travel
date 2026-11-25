package vn.edu.tvu.vinhlongtour.common;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

/**
 * Các cấu hình riêng của ứng dụng (nhóm {@code app:} trong application.yml),
 * phần lớn đọc từ biến môi trường để không phải ghi bí mật vào mã nguồn.
 */
@ConfigurationProperties("app")
public record AppProperties(Jwt jwt, Admin admin, Google google, Cors cors, Booking booking, PayOs payos, Upload upload) {

    /** Thư mục lưu ảnh admin tải lên; ảnh được phục vụ ở đường dẫn {@code /uploads/<tên file>}. */
    public record Upload(Path dir) {}

    /**
     * Cấu hình cổng payOS: ba khóa lấy ở kênh thanh toán trên my.payos.vn; {@code returnUrl}/{@code cancelUrl} là trang
     * frontend payOS chuyển khách về sau khi trả tiền/hủy; link thanh toán hết hạn sau {@code linkTimeout}.
     */
    public record PayOs(String clientId, String apiKey, String checksumKey, String apiUrl, String returnUrl, String cancelUrl,
                        Duration linkTimeout) {}

    /** Thời gian giữ chỗ khi chưa thanh toán, và phần trăm đặt cọc tối thiểu. */
    public record Booking(Duration hold, int depositPercent) {}

    /** Khóa ký token đăng nhập và thời gian sống của token. */
    public record Jwt(String secret, Duration ttl) {}

    /** Tài khoản admin được tạo lần đầu khi CSDL chưa có admin nào. */
    public record Admin(String email, String password) {}

    /** Client ID của ứng dụng trên Google Cloud, dùng để kiểm tra token đăng nhập Google. */
    public record Google(String clientId) {}

    /** Các địa chỉ frontend được phép gọi API từ trình duyệt. */
    public record Cors(List<String> allowedOrigins) {}
}
