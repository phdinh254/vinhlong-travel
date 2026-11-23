package vn.edu.tvu.vinhlongtour.catalog;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import vn.edu.tvu.vinhlongtour.TestcontainersConfiguration;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static vn.edu.tvu.vinhlongtour.TestAuth.adminToken;
import static vn.edu.tvu.vinhlongtour.TestAuth.bearer;
import static vn.edu.tvu.vinhlongtour.TestAuth.customerToken;

/**
 * Kiểm tra API danh mục trên MySQL thật, gồm 38 điểm đến nạp từ Flyway:
 * khách xem được không cần đăng nhập, lọc đúng, admin thêm/sửa/xóa và các quy tắc nghiệp vụ.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class CatalogIntegrationTests {

    private static final String SEEDED_CODE = "(VL|BT|TV)\\d\\d";

    @Autowired
    MockMvc mvc;

    // ---------- Dữ liệu điểm đến đã nạp ----------

    @Test
    void all38SeededDestinationsArePublic() throws Exception {
        assertThat(seededCodes(mvc.perform(get("/api/v1/destinations").param("size", "100")))).hasSize(38);
    }

    @Test
    void filterByRegionReturnsOnlyThatRegion() throws Exception {
        long benTre = idByName("/api/v1/regions", "Bến Tre (cũ)");

        List<String> codes = seededCodes(mvc.perform(get("/api/v1/destinations").param("regionId", String.valueOf(benTre)).param("size", "100")));

        assertThat(codes).hasSize(13).allMatch(c -> c.startsWith("BT"));
    }

    @Test
    void filterByCategoryFindsTheBeach() throws Exception {
        long beach = idByName("/api/v1/tour-categories", "Biển");

        assertThat(seededCodes(mvc.perform(get("/api/v1/destinations").param("categoryId", String.valueOf(beach))))).containsExactly("TV05");
    }

    @Test
    void destinationDetailHasScheduleAndCategories() throws Exception {
        long id = destinationIdByCode("VL01");

        mvc.perform(get("/api/v1/destinations/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Văn Thánh Miếu Vĩnh Long"))
                .andExpect(jsonPath("$.closedDays[0]").value("SUNDAY"))
                .andExpect(jsonPath("$.openTime").value("08:00:00"))
                .andExpect(jsonPath("$.categories[*].name", hasItem("Lịch sử")))
                .andExpect(jsonPath("$.region.name").value("Vĩnh Long (cũ)"));
    }

    @Test
    void brokenTokenDoesNotBlockPublicPages() throws Exception {
        mvc.perform(get("/api/v1/regions").header("Authorization", "Bearer token-cu-da-het-han"))
                .andExpect(status().isOk());
    }

    // ---------- Danh mục chỉ có tên ----------

    @Test
    void customerCannotChangeCatalogs() throws Exception {
        mvc.perform(post("/api/v1/admin/regions").header("Authorization", bearer(customerToken(mvc)))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Khu lạ\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminManagesRegionsAndCannotDeleteOneInUse() throws Exception {
        String admin = bearer(adminToken(mvc));
        String name = "Khu thử " + UUID.randomUUID();

        String created = mvc.perform(post("/api/v1/admin/regions").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("name", name))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        long id = ((Number) JsonPath.read(created, "$.id")).longValue();

        mvc.perform(post("/api/v1/admin/regions").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("name", name.toUpperCase()))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("NAME_TAKEN"));

        mvc.perform(put("/api/v1/admin/regions/{id}", id).header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("name", name + " (đổi)"))))
                .andExpect(status().isOk());
        mvc.perform(delete("/api/v1/admin/regions/{id}", id).header("Authorization", admin))
                .andExpect(status().isNoContent());

        // Khu vực đang có điểm đến thì CSDL chặn xóa
        mvc.perform(delete("/api/v1/admin/regions/{id}", idByName("/api/v1/regions", "Vĩnh Long (cũ)")).header("Authorization", admin))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DATA_CONFLICT"));
    }

    // ---------- Loại khách ----------

    @Test
    void passengerTypeAgeRangesMustNotOverlap() throws Exception {
        String admin = bearer(adminToken(mvc));

        mvc.perform(post("/api/v1/admin/passenger-types").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Thiếu niên\",\"minAge\":10,\"maxAge\":15,\"seats\":1}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("AGE_RANGE_OVERLAP"));
        mvc.perform(post("/api/v1/admin/passenger-types").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Sai tuổi\",\"minAge\":15,\"maxAge\":10,\"seats\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_AGE_RANGE"));
        mvc.perform(get("/api/v1/passenger-types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Người lớn"));
    }

    // ---------- Điểm đến ----------

    @Test
    void adminCreatesHidesAndValidatesDestinations() throws Exception {
        String admin = bearer(adminToken(mvc));
        String code = "T" + (System.nanoTime() % 1_000_000);

        mvc.perform(post("/api/v1/admin/destinations").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                        .content(destinationJson(code, "17:00", "08:00", true)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_OPENING_HOURS"));

        String created = mvc.perform(post("/api/v1/admin/destinations").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                        .content(destinationJson(code, "08:00", "17:00", true)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(code))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        long id = ((Number) JsonPath.read(created, "$.id")).longValue();

        mvc.perform(post("/api/v1/admin/destinations").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                        .content(destinationJson(code.toLowerCase(), "08:00", "17:00", true)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CODE_TAKEN"));

        // Ẩn điểm đến: khách không thấy nữa, admin vẫn thấy
        mvc.perform(put("/api/v1/admin/destinations/{id}", id).header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                        .content(destinationJson(code, "08:00", "17:00", false)))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/destinations/{id}", id)).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/admin/destinations/{id}", id).header("Authorization", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    // ---------- Chính sách hủy ----------

    @Test
    void cancellationPolicyIsVersionedAndLockedOnceEffective() throws Exception {
        String admin = bearer(adminToken(mvc));
        LocalDate today = LocalDate.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh"));

        mvc.perform(post("/api/v1/admin/cancellation-policies").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                        .content(policyJson("Sai mốc", today, "[{\"daysBeforeFrom\":0,\"daysBeforeTo\":5,\"penaltyPercent\":100},"
                                + "{\"daysBeforeFrom\":5,\"daysBeforeTo\":9,\"penaltyPercent\":50}]")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PENALTY_TIERS"));

        String effective = mvc.perform(post("/api/v1/admin/cancellation-policies").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                        .content(policyJson("Áp dụng từ hôm nay", today, "[{\"daysBeforeFrom\":0,\"daysBeforeTo\":2,\"penaltyPercent\":100}]")))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String future = mvc.perform(post("/api/v1/admin/cancellation-policies").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                        .content(policyJson("Áp dụng từ tuần sau", today.plusDays(7), "[{\"daysBeforeFrom\":0,\"daysBeforeTo\":3,\"penaltyPercent\":100}]")))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Khách thấy chính sách đang áp dụng, chưa thấy bản của tuần sau
        mvc.perform(get("/api/v1/cancellation-policies/current"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Áp dụng từ hôm nay"));

        mvc.perform(put("/api/v1/admin/cancellation-policies/{id}", (Object) JsonPath.read(effective, "$.id")).header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(policyJson("Sửa lén", today, "[{\"daysBeforeFrom\":0,\"daysBeforeTo\":2,\"penaltyPercent\":10}]")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("POLICY_ALREADY_EFFECTIVE"));
        mvc.perform(put("/api/v1/admin/cancellation-policies/{id}", (Object) JsonPath.read(future, "$.id")).header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(policyJson("Áp dụng từ tuần sau (sửa)", today.plusDays(7), "[{\"daysBeforeFrom\":0,\"daysBeforeTo\":3,\"penaltyPercent\":90}]")))
                .andExpect(status().isOk());
    }

    // ---------- Phụ thu, điểm đón ----------

    @Test
    void inactiveSurchargeIsHiddenFromCustomers() throws Exception {
        String admin = bearer(adminToken(mvc));
        String name = "Phòng đơn " + UUID.randomUUID();
        String body = "{\"name\":\"%s\",\"method\":\"PER_UNIT\",\"unitPrice\":250000,\"active\":%s}";

        String created = mvc.perform(post("/api/v1/admin/surcharges").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                        .content(body.formatted(name, true)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        mvc.perform(get("/api/v1/surcharges")).andExpect(jsonPath("$[*].name", hasItem(name)));

        mvc.perform(put("/api/v1/admin/surcharges/{id}", (Object) JsonPath.read(created, "$.id")).header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content(body.formatted(name, false)))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/surcharges")).andExpect(jsonPath("$[*].name", not(hasItem(name))));
    }

    @Test
    void pickupPointsAreAdminOnly() throws Exception {
        mvc.perform(get("/api/v1/admin/pickup-points")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/admin/pickup-points").header("Authorization", bearer(adminToken(mvc))).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Bến xe Vĩnh Long " + UUID.randomUUID() + "\",\"address\":\"QL1A, phường Tân Ngãi\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.active").value(true));
    }

    // ---------- Hàm phụ ----------

    private List<String> seededCodes(ResultActions result) throws Exception {
        List<String> codes = JsonPath.read(result.andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8),
                "$.items[*].code");
        return codes.stream().filter(c -> c.matches(SEEDED_CODE)).toList();
    }

    private long idByName(String url, String name) throws Exception {
        String json = mvc.perform(get(url)).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        List<Number> ids = JsonPath.read(json, "$[?(@.name == '" + name + "')].id");
        return ids.getFirst().longValue();
    }

    private long destinationIdByCode(String code) throws Exception {
        String json = mvc.perform(get("/api/v1/destinations").param("size", "100"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        List<Number> ids = JsonPath.read(json, "$.items[?(@.code == '" + code + "')].id");
        return ids.getFirst().longValue();
    }

    private String destinationJson(String code, String open, String close, boolean active) throws Exception {
        return """
                {"code":"%s","name":"Điểm thử %s","regionId":%d,"address":"Ấp thử",
                 "latitude":10.25,"longitude":105.97,"coordinateQuality":"APPROXIMATE",
                 "categoryIds":[%d],"openTime":"%s","closeTime":"%s","closedDays":["MONDAY"],"hoursQuality":"ESTIMATED",
                 "ticketPriceAdult":30000,"ticketPriceChild":15000,"priceQuality":"ESTIMATED",
                 "visitMinutes":60,"popularity":3,"active":%s}
                """.formatted(code, code, idByName("/api/v1/regions", "Vĩnh Long (cũ)"),
                idByName("/api/v1/tour-categories", "Sinh thái"), open, close, active);
    }

    private static String policyJson(String name, LocalDate effectiveFrom, String tiers) {
        return "{\"name\":\"%s\",\"effectiveFrom\":\"%s\",\"tiers\":%s}".formatted(name, effectiveFrom, tiers);
    }

    private static String json(Map<String, String> fields) {
        return fields.entrySet().stream().map(e -> "\"" + e.getKey() + "\":\"" + e.getValue() + "\"")
                .collect(java.util.stream.Collectors.joining(",", "{", "}"));
    }
}
