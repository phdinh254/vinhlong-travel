package vn.edu.tvu.vinhlongtour;

import com.jayway.jsonpath.JsonPath;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Hàm phụ cho test tích hợp: lấy token admin (tài khoản tạo từ application-test.yml)
 * hoặc đăng ký một khách hàng mới để lấy token.
 */
public final class TestAuth {

    private TestAuth() {
    }

    public static String adminToken(MockMvc mvc) throws Exception {
        return token(mvc, "/api/v1/auth/login", "{\"email\":\"admin@test.vn\",\"password\":\"Admin@12345\"}", 200);
    }

    public static String customerToken(MockMvc mvc) throws Exception {
        String body = "{\"email\":\"kh-%s@test.vn\",\"password\":\"MatKhau@123\",\"fullName\":\"Khách thử\"}".formatted(UUID.randomUUID());
        return token(mvc, "/api/v1/auth/register", body, 201);
    }

    public static String bearer(String token) {
        return "Bearer " + token;
    }

    private static String token(MockMvc mvc, String url, String body, int expectedStatus) throws Exception {
        String json = mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().is(expectedStatus))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return JsonPath.read(json, "$.token");
    }
}
