package vn.edu.tvu.vinhlongtour.auth.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;
import vn.edu.tvu.vinhlongtour.common.ApiException;
import vn.edu.tvu.vinhlongtour.common.AppProperties;

import java.util.Set;

/**
 * Kiểm tra "ID token" mà nút Đăng nhập Google ở frontend trả về: chữ ký đúng khóa công khai
 * của Google và còn hạn (do bộ giải mã kiểm tra), do Google cấp, cấp cho đúng ứng dụng
 * của mình (client ID), và email đã được Google xác minh.
 */
@Component
public class GoogleTokenVerifier {

    private static final String GOOGLE_KEYS_URL = "https://www.googleapis.com/oauth2/v3/certs";
    private static final Set<String> GOOGLE_ISSUERS = Set.of("accounts.google.com", "https://accounts.google.com");

    private final String clientId;
    private final JwtDecoder decoder;

    @Autowired
    public GoogleTokenVerifier(AppProperties props) {
        this(props.google().clientId(), NimbusJwtDecoder.withJwkSetUri(GOOGLE_KEYS_URL).build());
    }

    /** Cho phép test thay bộ giải mã, không cần gọi tới Google. */
    GoogleTokenVerifier(String clientId, JwtDecoder decoder) {
        this.clientId = clientId;
        this.decoder = decoder;
    }

    public GoogleUser verify(String idToken) {
        if (clientId == null || clientId.isBlank()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "GOOGLE_NOT_CONFIGURED",
                    "Máy chủ chưa cấu hình đăng nhập Google (GOOGLE_CLIENT_ID)");
        }
        Jwt jwt;
        try {
            jwt = decoder.decode(idToken);
        } catch (JwtException e) {
            throw invalidToken();
        }
        // Token Google cấp cho ứng dụng khác không được dùng để đăng nhập vào hệ thống này
        if (!GOOGLE_ISSUERS.contains(jwt.getClaimAsString(JwtClaimNames.ISS))
                || jwt.getAudience() == null || !jwt.getAudience().contains(clientId)
                || jwt.getClaimAsString("email") == null) {
            throw invalidToken();
        }
        if (!Boolean.TRUE.equals(jwt.getClaimAsBoolean("email_verified"))) {
            throw ApiException.unauthorized("GOOGLE_EMAIL_NOT_VERIFIED", "Email Google chưa được xác minh");
        }
        return new GoogleUser(jwt.getSubject(), jwt.getClaimAsString("email"),
                jwt.getClaimAsString("name"), jwt.getClaimAsString("picture"));
    }

    private static ApiException invalidToken() {
        return ApiException.unauthorized("INVALID_GOOGLE_TOKEN", "Token đăng nhập Google không hợp lệ hoặc đã hết hạn");
    }

    /** Thông tin người dùng lấy từ token Google đã kiểm tra. */
    public record GoogleUser(String sub, String email, String name, String picture) {}
}
