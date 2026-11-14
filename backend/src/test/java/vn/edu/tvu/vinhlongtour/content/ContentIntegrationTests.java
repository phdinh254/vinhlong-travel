package vn.edu.tvu.vinhlongtour.content;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import vn.edu.tvu.vinhlongtour.TestcontainersConfiguration;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static vn.edu.tvu.vinhlongtour.TestAuth.adminToken;
import static vn.edu.tvu.vinhlongtour.TestAuth.bearer;
import static vn.edu.tvu.vinhlongtour.TestAuth.customerToken;

/**
 * Kiểm tra tin tức, cấu hình trang, tin nhắn liên hệ và upload ảnh trên PostgreSQL thật.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class ContentIntegrationTests {

    @Autowired
    MockMvc mvc;

    private String admin;

    @BeforeEach
    void setUp() throws Exception {
        admin = bearer(adminToken(mvc));
    }

    private static String article(String title, String slug, String status) {
        return """
                {"title":"%s","slug":%s,"summary":"Tóm tắt","content":"<p>Nội dung</p>","category":"CULTURE","status":"%s"}
                """.formatted(title, slug == null ? "null" : "\"" + slug + "\"", status);
    }

    private ResultActions createArticle(String body) throws Exception {
        return mvc.perform(post("/api/v1/admin/articles").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private static String body(ResultActions r) throws Exception {
        return r.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    // ---------- Tin tức ----------

    @Test
    void draftIsHiddenUntilPublishedAndSlugComesFromTitle() throws Exception {
        String title = "Lễ hội Ok Om Bok " + UUID.randomUUID().toString().substring(0, 6);
        String json = body(createArticle(article(title, null, "DRAFT")).andExpect(status().isCreated())
                .andExpect(jsonPath("$.publishedAt").doesNotExist()));
        long id = ((Number) JsonPath.read(json, "$.id")).longValue();
        String slug = JsonPath.read(json, "$.slug");
        org.assertj.core.api.Assertions.assertThat(slug).startsWith("le-hoi-ok-om-bok-");

        mvc.perform(get("/api/v1/articles/{slug}", slug)).andExpect(status().isNotFound());

        // Sửa không gửi slug: giữ slug cũ dù đổi tiêu đề, để link đã chia sẻ vẫn chạy
        mvc.perform(put("/api/v1/admin/articles/{id}", id).header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                        .content(article("Tiêu đề mới hẳn", null, "PUBLISHED")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value(slug))
                .andExpect(jsonPath("$.publishedAt").exists());

        mvc.perform(get("/api/v1/articles/{slug}", slug)).andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Tiêu đề mới hẳn"))
                .andExpect(jsonPath("$.viewCount").value(1));
        mvc.perform(get("/api/v1/articles/{slug}", slug)).andExpect(jsonPath("$.viewCount").value(2));
        mvc.perform(get("/api/v1/articles").param("category", "CULTURE").param("size", "50"))
                .andExpect(jsonPath("$.items[?(@.id == " + id + ")].slug").value(slug));
        mvc.perform(get("/api/v1/articles").param("category", "EVENT").param("size", "50"))
                .andExpect(jsonPath("$.items[?(@.id == " + id + ")]").isEmpty());
    }

    @Test
    void republishingKeepsTheFirstPublishDate() throws Exception {
        String json = body(createArticle(article("Bài xuất bản " + UUID.randomUUID(), null, "PUBLISHED")));
        long id = ((Number) JsonPath.read(json, "$.id")).longValue();
        // Đọc lại từ CSDL: PostgreSQL chỉ lưu tới micro giây, còn đối tượng vừa tạo giữ độ chính xác của đồng hồ máy
        String publishedAt = JsonPath.read(body(mvc.perform(get("/api/v1/admin/articles/{id}", id).header("Authorization", admin))),
                "$.publishedAt");

        for (String status : new String[]{"DRAFT", "PUBLISHED"}) {
            mvc.perform(put("/api/v1/admin/articles/{id}", id).header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                    .content(article("Bài xuất bản lại", null, status))).andExpect(jsonPath("$.publishedAt").value(publishedAt));
        }
    }

    @Test
    void duplicateSlugs() throws Exception {
        String title = "Chợ nổi Trà Ôn " + UUID.randomUUID().toString().substring(0, 6);
        String first = JsonPath.read(body(createArticle(article(title, null, "DRAFT"))), "$.slug");
        String second = JsonPath.read(body(createArticle(article(title, null, "DRAFT"))), "$.slug");
        org.assertj.core.api.Assertions.assertThat(second).isEqualTo(first + "-2");

        createArticle(article("Khác", first, "DRAFT")).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("SLUG_TAKEN"));
        createArticle(article("Khác", "Co Dau", "DRAFT")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields.slug").exists());
    }

    @Test
    void adminDeletesArticleAndCustomerCannotManage() throws Exception {
        long id = ((Number) JsonPath.read(body(createArticle(article("Bài sẽ xóa", null, "DRAFT"))), "$.id")).longValue();
        String customer = bearer(customerToken(mvc));

        mvc.perform(get("/api/v1/admin/articles").header("Authorization", customer)).andExpect(status().isForbidden());
        mvc.perform(delete("/api/v1/admin/articles/{id}", id).header("Authorization", admin)).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/admin/articles/{id}", id).header("Authorization", admin)).andExpect(status().isNotFound());
    }

    // ---------- Cấu hình trang ----------

    @Test
    void settingsArePublicAndAdminUpdatesOnlyWhatIsSent() throws Exception {
        mvc.perform(put("/api/v1/admin/site-settings").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"HOTLINE\":\"0270 3 822 222\",\"FACEBOOK_URL\":\"https://facebook.com/vinhlongtour\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.HOTLINE").value("0270 3 822 222"));
        mvc.perform(put("/api/v1/admin/site-settings").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                .content("{\"ADDRESS\":\"Vĩnh Long\"}")).andExpect(status().isOk());

        mvc.perform(get("/api/v1/site-settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.HOTLINE").value("0270 3 822 222"))
                .andExpect(jsonPath("$.ADDRESS").value("Vĩnh Long"))
                .andExpect(jsonPath("$.TIKTOK_URL").value(""));
    }

    @Test
    void settingLinksMustBeHttp() throws Exception {
        mvc.perform(put("/api/v1/admin/site-settings").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"MAP_EMBED_URL\":\"javascript:alert(1)\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_LINK"));
        mvc.perform(put("/api/v1/admin/site-settings").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                .content("{\"KHONG_CO\":\"x\"}")).andExpect(status().isBadRequest());
        mvc.perform(put("/api/v1/admin/site-settings").header("Authorization", bearer(customerToken(mvc)))
                .contentType(MediaType.APPLICATION_JSON).content("{\"HOTLINE\":\"1\"}")).andExpect(status().isForbidden());
    }

    // ---------- Liên hệ ----------

    @Test
    void contactNeedsPhoneOrEmailAndAdminMarksHandled() throws Exception {
        mvc.perform(post("/api/v1/contact-messages").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Trần Văn L\",\"message\":\"Tư vấn giúp tôi\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("CONTACT_REQUIRED"));

        String json = body(mvc.perform(post("/api/v1/contact-messages").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Trần Văn L\",\"phone\":\"0909888777\",\"subject\":\"Tư vấn tour\",\"message\":\"Nhóm 10 người\"}"))
                .andExpect(status().isCreated()));
        long id = ((Number) JsonPath.read(json, "$.id")).longValue();

        mvc.perform(get("/api/v1/admin/contact-messages").param("status", "NEW").param("size", "100").header("Authorization", admin))
                .andExpect(jsonPath("$.items[?(@.id == " + id + ")].phone").value("0909888777"));
        mvc.perform(patch("/api/v1/admin/contact-messages/{id}/status", id).header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"HANDLED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("HANDLED"))
                .andExpect(jsonPath("$.handledBy").exists());
        mvc.perform(get("/api/v1/admin/contact-messages").param("status", "NEW").param("size", "100").header("Authorization", admin))
                .andExpect(jsonPath("$.items[?(@.id == " + id + ")]").isEmpty());
    }

    // ---------- Upload ảnh ----------

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0x0D, 'I', 'H', 'D', 'R'};

    @Test
    void uploadedImageIsServedBack() throws Exception {
        String json = body(mvc.perform(multipart("/api/v1/admin/uploads")
                        .file(new MockMultipartFile("file", "anh.jpg", "image/jpeg", PNG)).header("Authorization", admin))
                .andExpect(status().isCreated()));
        String url = JsonPath.read(json, "$.url");
        org.assertj.core.api.Assertions.assertThat(url).startsWith("/uploads/").endsWith(".png");     // đuôi theo nội dung thật

        mvc.perform(get(url)).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(content().bytes(PNG));
    }

    @Test
    void uploadRejectsNonImagesAndNonAdmins() throws Exception {
        byte[] html = "<html><script>alert(1)</script></html>".getBytes(StandardCharsets.UTF_8);
        mvc.perform(multipart("/api/v1/admin/uploads").file(new MockMultipartFile("file", "anh.jpg", "image/jpeg", html))
                        .header("Authorization", admin))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("NOT_AN_IMAGE"));
        mvc.perform(multipart("/api/v1/admin/uploads").file(new MockMultipartFile("file", "anh.png", "image/png", new byte[0]))
                        .header("Authorization", admin))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("FILE_REQUIRED"));
        mvc.perform(multipart("/api/v1/admin/uploads").file(new MockMultipartFile("file", "anh.png", "image/png", PNG))
                .header("Authorization", bearer(customerToken(mvc)))).andExpect(status().isForbidden());
        mvc.perform(multipart("/api/v1/admin/uploads").file(new MockMultipartFile("file", "anh.png", "image/png", PNG)))
                .andExpect(status().isUnauthorized());
    }
}
