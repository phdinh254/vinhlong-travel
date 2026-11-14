package vn.edu.tvu.vinhlongtour.auth.service;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import vn.edu.tvu.vinhlongtour.common.ApiException;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Kiểm tra các bước xác minh token Google mà không gọi tới Google:
 * thay bộ giải mã bằng một hàm trả về token dựng sẵn.
 */
class GoogleTokenVerifierTest {

    private static final String CLIENT_ID = "my-client-id";

    @Test
    void validTokenReturnsGoogleUser() {
        GoogleTokenVerifier verifier = new GoogleTokenVerifier(CLIENT_ID, decoderReturning("https://accounts.google.com", CLIENT_ID, true));

        var user = verifier.verify("token");

        assertThat(user.sub()).isEqualTo("google-123");
        assertThat(user.email()).isEqualTo("a@gmail.com");
    }

    @Test
    void tokenIssuedForAnotherAppIsRejected() {
        GoogleTokenVerifier verifier = new GoogleTokenVerifier(CLIENT_ID, decoderReturning("accounts.google.com", "ung-dung-khac", true));

        assertCode(() -> verifier.verify("token"), "INVALID_GOOGLE_TOKEN");
    }

    @Test
    void tokenNotIssuedByGoogleIsRejected() {
        GoogleTokenVerifier verifier = new GoogleTokenVerifier(CLIENT_ID, decoderReturning("https://ke-gia-mao.com", CLIENT_ID, true));

        assertCode(() -> verifier.verify("token"), "INVALID_GOOGLE_TOKEN");
    }

    @Test
    void unverifiedEmailIsRejected() {
        GoogleTokenVerifier verifier = new GoogleTokenVerifier(CLIENT_ID, decoderReturning("accounts.google.com", CLIENT_ID, false));

        assertCode(() -> verifier.verify("token"), "GOOGLE_EMAIL_NOT_VERIFIED");
    }

    @Test
    void badSignatureOrExpiredTokenIsRejected() {
        GoogleTokenVerifier verifier = new GoogleTokenVerifier(CLIENT_ID, token -> {
            throw new BadJwtException("chữ ký sai");
        });

        assertCode(() -> verifier.verify("token"), "INVALID_GOOGLE_TOKEN");
    }

    @Test
    void missingClientIdMeansGoogleLoginIsNotConfigured() {
        GoogleTokenVerifier verifier = new GoogleTokenVerifier("", decoderReturning("accounts.google.com", CLIENT_ID, true));

        assertCode(() -> verifier.verify("token"), "GOOGLE_NOT_CONFIGURED");
    }

    private static JwtDecoder decoderReturning(String issuer, String audience, boolean emailVerified) {
        return token -> Jwt.withTokenValue(token)
                .header("alg", "RS256")
                .issuer(issuer)
                .audience(List.of(audience))
                .subject("google-123")
                .claim("email", "a@gmail.com")
                .claim("email_verified", emailVerified)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }

    private static void assertCode(org.assertj.core.api.ThrowableAssert.ThrowingCallable call, String code) {
        assertThatThrownBy(call).isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getCode()).isEqualTo(code));
    }
}
