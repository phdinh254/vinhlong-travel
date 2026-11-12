package vn.edu.tvu.vinhlongtour.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        String frontendUrl,
        String corsAllowedOrigins,
        String uploadDir,
        boolean demoData,
        Jwt jwt,
        Google google,
        Admin admin,
        Booking booking,
        VnPay vnpay
) {
    public record Jwt(String secret, long expirationMinutes) {}
    public record Google(String clientId) {}
    public record Admin(String email, String password, String name) {}
    public record Booking(int holdMinutes, int depositPercent) {}
    public record VnPay(String tmnCode, String hashSecret, String payUrl, String returnUrl, boolean processOnReturn) {}
}
