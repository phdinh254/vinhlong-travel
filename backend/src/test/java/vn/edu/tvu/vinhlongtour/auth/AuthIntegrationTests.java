package vn.edu.tvu.vinhlongtour.auth;

import com.jayway.jsonpath.JsonPath;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import vn.edu.tvu.vinhlongtour.TestcontainersConfiguration;
import vn.edu.tvu.vinhlongtour.auth.service.GoogleTokenVerifier;
import vn.edu.tvu.vinhlongtour.auth.service.GoogleTokenVerifier.GoogleUser;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Kiểm tra toàn bộ luồng tài khoản trên PostgreSQL thật: đăng ký, đăng nhập, token sai/hết hạn,
 * phân quyền admin, khóa tài khoản có hiệu lực ngay, đổi mật khẩu, đăng nhập Google
 * (giả lập phần kiểm tra token của Google).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class AuthIntegrationTests {

    private static final String TEST_SECRET = "test-secret-chi-dung-cho-kiem-thu-0123456789";
    private static final String PASSWORD = "MatKhau@123";

    @Autowired
    MockMvc mvc;

    @MockitoBean
    GoogleTokenVerifier googleVerifier;

    // ---------- Đăng ký, đăng nhập ----------

    @Test
    void registeredUserCanUseTokenToReadProfile() throws Exception {
        String email = uniqueEmail();
        String token = token(register(email, PASSWORD));

        mvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.hasPassword").value(true));
    }

    @Test
    void emailIsUniqueRegardlessOfLetterCase() throws Exception {
        String email = uniqueEmail();
        register(email, PASSWORD);

        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson(email.toUpperCase(), PASSWORD)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_TAKEN"));
    }

    @Test
    void invalidRegistrationListsBadFields() throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("khong-phai-email", "ngan")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fields.email").exists())
                .andExpect(jsonPath("$.fields.password").exists());
    }

    @Test
    void wrongPasswordAndUnknownEmailGiveSameError() throws Exception {
        String email = uniqueEmail();
        register(email, PASSWORD);

        login(email, "SaiMatKhau@1").andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
        login(uniqueEmail(), PASSWORD).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    // ---------- Token ----------

    @Test
    void requestWithoutTokenIsRejected() throws Exception {
        mvc.perform(get("/api/v1/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void expiredTokenIsRejected() throws Exception {
        long id = accountId(register(uniqueEmail(), PASSWORD));
        Instant past = Instant.now().minus(3, ChronoUnit.HOURS);
        String expired = sign(new SecretKeySpec(TEST_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"), id, past);

        mvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + expired))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void tokenSignedWithAnotherKeyIsRejected() throws Exception {
        long id = accountId(register(uniqueEmail(), PASSWORD));
        byte[] otherKey = new byte[32];
        new SecureRandom().nextBytes(otherKey);
        String forged = sign(new SecretKeySpec(otherKey, "HmacSHA256"), id, Instant.now());

        mvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + forged))
                .andExpect(status().isUnauthorized());
    }

    // ---------- Phân quyền, khóa tài khoản ----------

    @Test
    void customerCannotUseAdminApi() throws Exception {
        String token = token(register(uniqueEmail(), PASSWORD));

        mvc.perform(get("/api/v1/admin/accounts").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void lockingAccountTakesEffectImmediatelyAndUnlockRestoresAccess() throws Exception {
        String email = uniqueEmail();
        String userJson = register(email, PASSWORD);
        String userToken = token(userJson);
        long userId = accountId(userJson);
        String adminToken = adminToken();

        changeStatus(adminToken, userId, "LOCKED").andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("LOCKED"));

        // Token cấp trước khi khóa vẫn còn hạn nhưng bị từ chối ngay
        mvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCOUNT_LOCKED"));
        login(email, PASSWORD).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCOUNT_LOCKED"));

        changeStatus(adminToken, userId, "ACTIVE").andExpect(status().isOk());
        mvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());
    }

    @Test
    void adminCannotLockOwnAccount() throws Exception {
        String adminJson = loginJson("admin@test.vn", "Admin@12345");

        changeStatus(token(adminJson), accountId(adminJson), "LOCKED")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CANNOT_LOCK_SELF"));
    }

    @Test
    void adminCanSearchAccountsByEmail() throws Exception {
        String email = uniqueEmail();
        register(email, PASSWORD);

        mvc.perform(get("/api/v1/admin/accounts").param("q", email).header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].email").value(email));
    }

    // ---------- Hồ sơ, mật khẩu ----------

    @Test
    void userCanUpdateProfile() throws Exception {
        String token = token(register(uniqueEmail(), PASSWORD));

        mvc.perform(put("/api/v1/me").header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Trần Thị B\",\"phone\":\"0901234567\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Trần Thị B"))
                .andExpect(jsonPath("$.phone").value("0901234567"));
    }

    @Test
    void changingPasswordRequiresCurrentPasswordAndReplacesOldOne() throws Exception {
        String email = uniqueEmail();
        String token = token(register(email, PASSWORD));

        changePassword(token, "SaiMatKhau@1", "MatKhauMoi@456").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("WRONG_PASSWORD"));
        changePassword(token, PASSWORD, "MatKhauMoi@456").andExpect(status().isNoContent());

        login(email, PASSWORD).andExpect(status().isUnauthorized());
        login(email, "MatKhauMoi@456").andExpect(status().isOk());
    }

    @Test
    void passwordLongerThan72BytesIsRejectedNotCrashing() throws Exception {
        // 40 ký tự có dấu = hơn 72 byte UTF-8: qua được kiểm tra độ dài ký tự nhưng BCrypt không xử lý được
        String longPassword = "ệ".repeat(40);
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson(uniqueEmail(), longPassword)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PASSWORD_TOO_LONG"));
    }

    // ---------- Google ----------

    @Test
    void firstGoogleLoginCreatesCustomerWithoutPassword() throws Exception {
        String email = uniqueEmail();
        String sub = UUID.randomUUID().toString();
        when(googleVerifier.verify(anyString())).thenReturn(new GoogleUser(sub, email, "Lê Văn C", "https://anh/c.png"));

        String first = googleLogin().andExpect(status().isOk())
                .andExpect(jsonPath("$.account.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.account.hasPassword").value(false))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Đăng nhập lại cùng tài khoản Google thì ra đúng tài khoản cũ, không tạo thêm
        String second = googleLogin().andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        org.assertj.core.api.Assertions.assertThat(accountId(second)).isEqualTo(accountId(first));
    }

    @Test
    void googleLoginLinksToExistingAccountWithSameEmail() throws Exception {
        String email = uniqueEmail();
        long existingId = accountId(register(email, PASSWORD));
        when(googleVerifier.verify(anyString()))
                .thenReturn(new GoogleUser(UUID.randomUUID().toString(), email.toUpperCase(), "Tên Google", null));

        googleLogin().andExpect(status().isOk())
                .andExpect(jsonPath("$.account.id").value(existingId))
                .andExpect(jsonPath("$.account.hasPassword").value(true));
    }

    // ---------- Hàm phụ ----------

    private static String uniqueEmail() {
        return "kh-" + UUID.randomUUID() + "@test.vn";
    }

    private static String registerJson(String email, String password) {
        return "{\"email\":\"%s\",\"password\":\"%s\",\"fullName\":\"Nguyễn Văn A\"}".formatted(email, password);
    }

    private String register(String email, String password) throws Exception {
        return mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(registerJson(email, password)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private ResultActions login(String email, String password) throws Exception {
        return mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)));
    }

    private String loginJson(String email, String password) throws Exception {
        return login(email, password).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private String adminToken() throws Exception {
        return token(loginJson("admin@test.vn", "Admin@12345"));
    }

    private ResultActions changeStatus(String adminToken, long accountId, String newStatus) throws Exception {
        return mvc.perform(patch("/api/v1/admin/accounts/{id}/status", accountId).header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"%s\"}".formatted(newStatus)));
    }

    private ResultActions changePassword(String token, String current, String next) throws Exception {
        return mvc.perform(put("/api/v1/me/password").header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\":\"%s\",\"newPassword\":\"%s\"}".formatted(current, next)));
    }

    private ResultActions googleLogin() throws Exception {
        return mvc.perform(post("/api/v1/auth/google").contentType(MediaType.APPLICATION_JSON).content("{\"credential\":\"gia-lap\"}"));
    }

    private static String token(String authJson) {
        return JsonPath.read(authJson, "$.token");
    }

    private static long accountId(String authJson) {
        return ((Number) JsonPath.read(authJson, "$.account.id")).longValue();
    }

    /** Tự ký một token với khóa và thời điểm phát hành tùy chọn (hạn dùng 2 giờ kể từ lúc phát hành). */
    private static String sign(SecretKey key, long accountId, Instant issuedAt) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("vinhlong-tour")
                .subject(String.valueOf(accountId))
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plus(2, ChronoUnit.HOURS))
                .build();
        return new NimbusJwtEncoder(new ImmutableSecret<>(key))
                .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }
}
