package vn.edu.tvu.vinhlongtour.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.edu.tvu.vinhlongtour.config.AppProperties;
import vn.edu.tvu.vinhlongtour.exception.ApiException;

import java.util.List;

/**
 * Xác thực Google ID token (chữ ký, issuer, audience = client ID, hạn dùng)
 * bằng thư viện chính thức google-api-client.
 */
@Slf4j
@Service
public class GoogleTokenService {
    private final GoogleIdTokenVerifier verifier;
    private final String clientId;

    public record GoogleUser(String sub, String email, boolean emailVerified, String name, String picture) {}

    public GoogleTokenService(AppProperties props) {
        this.clientId = props.google().clientId();
        this.verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(List.of(clientId == null ? "" : clientId))
                .build();
    }

    public GoogleUser verify(String credential) {
        if (clientId == null || clientId.isBlank()) {
            throw ApiException.badRequest("Máy chủ chưa cấu hình GOOGLE_CLIENT_ID");
        }
        try {
            GoogleIdToken token = verifier.verify(credential);
            if (token == null) throw ApiException.unauthorized("Token Google không hợp lệ");
            GoogleIdToken.Payload p = token.getPayload();
            return new GoogleUser(p.getSubject(), p.getEmail(), Boolean.TRUE.equals(p.getEmailVerified()),
                    (String) p.get("name"), (String) p.get("picture"));
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Lỗi xác thực Google token: {}", e.getMessage());
            throw ApiException.unauthorized("Không xác thực được tài khoản Google");
        }
    }
}
