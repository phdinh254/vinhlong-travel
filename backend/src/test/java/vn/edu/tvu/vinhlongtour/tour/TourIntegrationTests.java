package vn.edu.tvu.vinhlongtour.tour;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
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
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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
 * Kiểm tra luồng tour và đợt khởi hành trên PostgreSQL thật: soạn tour, mở bán, tạo đợt
 * (sao chép lịch trình mẫu), khách tìm/lọc và xem, các quy tắc chặn sai sót.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class TourIntegrationTests {

    @Autowired
    MockMvc mvc;

    private String admin;
    private String code;

    @BeforeEach
    void setUp() throws Exception {
        admin = bearer(adminToken(mvc));
        code = "T" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    @Test
    void draftTourIsHiddenUntilCompleteAndPublished() throws Exception {
        long tourId = createTour(code, null);

        mvc.perform(get("/api/v1/tours/{id}", tourId)).andExpect(status().isNotFound());
        mvc.perform(put("/api/v1/admin/tours/{id}", tourId).header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content(tourJson(code, "ON_SALE")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("TOUR_INCOMPLETE"));

        publish(tourId);

        mvc.perform(get("/api/v1/tours/{id}", tourId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.images[0].cover").value(true))
                .andExpect(jsonPath("$.itinerary.days.length()").value(2))
                .andExpect(jsonPath("$.itinerary.days[0].activities[0].destination.code").value("VL01"))
                .andExpect(jsonPath("$.includedAmenities[0].name").exists());
    }

    @Test
    void departureCopiesItineraryAndKeepsItWhenTemplateChanges() throws Exception {
        long tourId = createTour(code, null);
        publish(tourId);
        long departureId = createDeparture(tourId, 10).andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(org.hamcrest.Matchers.startsWith(code + "-")))
                .andExpect(jsonPath("$.seatsLeft").value(25))
                .andExpect(jsonPath("$.bookable").value(true))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8).transform(j -> ((Number) JsonPath.read(j, "$.id")).longValue());

        // Đổi lịch trình mẫu sau khi đã tạo đợt: lịch trình của đợt vẫn giữ nguyên
        mvc.perform(put("/api/v1/admin/tours/{id}/itinerary", tourId).header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                        .content(itineraryJson("Lịch trình mới ngày 1", "Lịch trình mới ngày 2")))
                .andExpect(status().isOk());

        mvc.perform(get("/api/v1/departures/{id}/itinerary", departureId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.days[0].title").value("Vĩnh Long"))
                .andExpect(jsonPath("$.days[1].title").value("Trà Vinh"));
    }

    @Test
    void departureNeedsPriceForEveryPassengerTypeAndValidSchedule() throws Exception {
        long tourId = createTour(code, null);
        List<Number> typeIds = passengerTypeIds();
        LocalDateTime start = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusDays(20).truncatedTo(ChronoUnit.HOURS);

        mvc.perform(post("/api/v1/admin/tours/{id}/departures", tourId).header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                        .content(departureJson(start, "[{\"passengerTypeId\":%d,\"price\":900000}]".formatted(typeIds.getFirst().longValue()))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MISSING_PRICE"));

        mvc.perform(post("/api/v1/admin/tours/{id}/departures", tourId).header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                        .content(departureJson(LocalDateTime.now().minusDays(1), allPrices())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("DEPARTURE_IN_PAST"));
    }

    @Test
    void customerFindsTourByFiltersAndSeesFromPrice() throws Exception {
        long tourId = createTour(code, null);
        publish(tourId);
        createDeparture(tourId, 30).andExpect(status().isCreated());
        createDeparture(tourId, 15).andExpect(status().isCreated());
        long vl01 = destinationId("VL01");

        String json = mvc.perform(get("/api/v1/tours").param("destinationId", String.valueOf(vl01)).param("minDays", "2").param("size", "50"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        List<Number> fromPrices = JsonPath.read(json, "$.items[?(@.code == '" + code + "')].fromPrice");
        List<String> next = JsonPath.read(json, "$.items[?(@.code == '" + code + "')].nextDepartureAt");

        assertThat(fromPrices).singleElement().satisfies(p -> assertThat(p.longValue()).isEqualTo(900_000L)); // giá người lớn
        assertThat(LocalDateTime.parse(next.getFirst()).toLocalDate())
                .isEqualTo(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusDays(15).toLocalDate());

        mvc.perform(get("/api/v1/tours").param("maxDays", "1").param("size", "50"))
                .andExpect(jsonPath("$.items[*].code", not(hasItem(code))));
    }

    @Test
    void tourWithDeparturesCannotBeDeletedButEmptyDepartureCan() throws Exception {
        long tourId = createTour(code, null);
        long departureId = ((Number) JsonPath.read(createDeparture(tourId, 12).andReturn().getResponse()
                .getContentAsString(StandardCharsets.UTF_8), "$.id")).longValue();

        mvc.perform(delete("/api/v1/admin/tours/{id}", tourId).header("Authorization", admin))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DATA_CONFLICT"));
        mvc.perform(delete("/api/v1/admin/departures/{id}", departureId).header("Authorization", admin))
                .andExpect(status().isNoContent());
        mvc.perform(delete("/api/v1/admin/tours/{id}", tourId).header("Authorization", admin))
                .andExpect(status().isNoContent());
    }

    @Test
    void customerCannotManageTours() throws Exception {
        mvc.perform(post("/api/v1/admin/tours").header("Authorization", bearer(customerToken(mvc)))
                        .contentType(MediaType.APPLICATION_JSON).content(tourJson(code, null)))
                .andExpect(status().isForbidden());
    }

    // ---------- Hàm phụ ----------

    private long createTour(String code, String status) throws Exception {
        String json = mvc.perform(post("/api/v1/admin/tours").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content(tourJson(code, status)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }

    /** Thêm ảnh, lịch trình 2 ngày rồi chuyển sang đang bán. */
    private void publish(long tourId) throws Exception {
        mvc.perform(put("/api/v1/admin/tours/{id}/images", tourId).header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"images\":[{\"url\":\"/uploads/cu-lao.jpg\",\"caption\":\"Cù lao\"},{\"url\":\"https://anh.vn/b.jpg\"}]}"))
                .andExpect(status().isOk());
        mvc.perform(put("/api/v1/admin/tours/{id}/itinerary", tourId).header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                        .content(itineraryJson("Vĩnh Long", "Trà Vinh")))
                .andExpect(status().isOk());
        mvc.perform(put("/api/v1/admin/tours/{id}", tourId).header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content(tourJson(code, "ON_SALE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ON_SALE"));
    }

    private ResultActions createDeparture(long tourId, int daysFromNow) throws Exception {
        LocalDateTime start = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusDays(daysFromNow).truncatedTo(ChronoUnit.HOURS);
        return mvc.perform(post("/api/v1/admin/tours/{id}/departures", tourId).header("Authorization", admin)
                .contentType(MediaType.APPLICATION_JSON).content(departureJson(start, allPrices())));
    }

    private String tourJson(String code, String status) throws Exception {
        String amenity = mvc.perform(post("/api/v1/admin/amenities").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Xe đưa đón " + UUID.randomUUID() + "\"}"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return """
                {"code":"%s","name":"Tour thử %s","summary":"Tóm tắt","durationDays":2,"durationNights":1,
                 "regionIds":[%d,%d],"categoryIds":[%d],"amenities":[{"amenityId":%d,"included":true}],
                 "featured":true%s}
                """.formatted(code, code, idByName("/api/v1/regions", "Vĩnh Long (cũ)"), idByName("/api/v1/regions", "Trà Vinh (cũ)"),
                idByName("/api/v1/tour-categories", "Văn hóa"), ((Number) JsonPath.read(amenity, "$.id")).longValue(),
                status == null ? "" : ",\"status\":\"" + status + "\"");
    }

    private String itineraryJson(String day1, String day2) throws Exception {
        return """
                {"days":[
                  {"title":"%s","activities":[{"startTime":"08:00","title":"Tham quan Văn Thánh Miếu","destinationId":%d},
                                              {"startTime":"11:30","title":"Ăn trưa"}]},
                  {"title":"%s","activities":[{"startTime":"08:00","title":"Ao Bà Om","destinationId":%d}]}]}
                """.formatted(day1, destinationId("VL01"), day2, destinationId("TV01"));
    }

    private String allPrices() throws Exception {
        List<Number> ids = passengerTypeIds();       // sắp theo tuổi giảm dần: người lớn, trẻ em, em bé
        long[] prices = {900_000, 450_000, 0};
        return java.util.stream.IntStream.range(0, ids.size())
                .mapToObj(i -> "{\"passengerTypeId\":%d,\"price\":%d}".formatted(ids.get(i).longValue(), prices[Math.min(i, 2)]))
                .collect(Collectors.joining(",", "[", "]"));
    }

    private static String departureJson(LocalDateTime start, String prices) {
        return """
                {"departureAt":"%s","returnAt":"%s","bookingDeadline":"%s","capacity":25,"prices":%s}
                """.formatted(start, start.plusDays(1).plusHours(10), start.minusDays(1), prices);
    }

    private List<Number> passengerTypeIds() throws Exception {
        return JsonPath.read(mvc.perform(get("/api/v1/passenger-types")).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$[*].id");
    }

    private long destinationId(String code) throws Exception {
        String json = mvc.perform(get("/api/v1/destinations").param("q", "").param("size", "100"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        List<Number> ids = JsonPath.read(json, "$.items[?(@.code == '" + code + "')].id");
        return ids.getFirst().longValue();
    }

    private long idByName(String url, String name) throws Exception {
        List<Number> ids = JsonPath.read(mvc.perform(get(url)).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8),
                "$[?(@.name == '" + name + "')].id");
        return ids.getFirst().longValue();
    }
}
