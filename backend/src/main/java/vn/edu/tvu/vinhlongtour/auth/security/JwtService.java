package vn.edu.tvu.vinhlongtour.auth.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Service;
import vn.edu.tvu.vinhlongtour.auth.domain.Account;
import vn.edu.tvu.vinhlongtour.common.AppProperties;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;

/**
 * Cấp và kiểm tra token đăng nhập (JWT ký bằng HMAC-SHA256).
 * Token chỉ mang mã tài khoản; vai trò và trạng thái khóa luôn đọc lại từ CSDL
 * ở mỗi request (xem {@link AccountJwtConverter}), nên khóa tài khoản có hiệu lực ngay.
 */
@Service
public class JwtService {

    static final String ISSUER = "vinhlong-tour";
    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    private final SecretKey key;
    private final Duration ttl;
    private final NimbusJwtEncoder encoder;

    public JwtService(AppProperties props) {
        this.key = secretKey(props.jwt().secret());
        this.ttl = props.jwt().ttl();
        this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
    }

    /**
     * Không có giá trị mặc định cho khóa: để trống thì sinh khóa ngẫu nhiên mỗi lần chạy
     * (token cũ hết hiệu lực khi khởi động lại), tránh dùng một khóa ai cũng biết.
     */
    static SecretKey secretKey(String secret) {
        byte[] bytes;
        if (secret == null || secret.isBlank()) {
            log.warn("Chưa đặt JWT_SECRET: dùng khóa ngẫu nhiên, mọi token sẽ mất hiệu lực khi khởi động lại backend");
            bytes = new byte[32];
            new SecureRandom().nextBytes(bytes);
        } else {
            bytes = secret.getBytes(StandardCharsets.UTF_8);
            if (bytes.length < 32) {
                throw new IllegalStateException("JWT_SECRET phải dài ít nhất 32 ký tự");
            }
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    public IssuedToken issue(Account account) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(ttl);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .subject(String.valueOf(account.getId()))
                .issuedAt(now)
                .expiresAt(expiresAt)
                .build();
        String token = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return new IssuedToken(token, expiresAt);
    }

    public JwtDecoder decoder() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(ISSUER));
        return decoder;
    }

    /** Token vừa cấp kèm thời điểm hết hạn để frontend biết khi nào cần đăng nhập lại. */
    public record IssuedToken(String value, Instant expiresAt) {}
}
