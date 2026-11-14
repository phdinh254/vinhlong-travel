package vn.edu.tvu.vinhlongtour.planner;

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
import org.springframework.transaction.annotation.Transactional;
import vn.edu.tvu.vinhlongtour.TestTours;
import vn.edu.tvu.vinhlongtour.TestcontainersConfiguration;
import vn.edu.tvu.vinhlongtour.catalog.domain.Destination;
import vn.edu.tvu.vinhlongtour.catalog.repository.DestinationRepository;

import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
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
 * Kiểm tra API gợi ý lịch trình trên 38 điểm đến thật (nạp sẵn bằng Flyway): các luật về giờ mở cửa,
 * ngày đóng cửa, bữa trưa, quỹ giờ, ngân sách; gợi ý tour giống nhất; lưu và xem lại lịch trình.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class PlannerIntegrationTests {

    @Autowired
    MockMvc mvc;

    @Autowired
    DestinationRepository destinations;

    private static LocalDate today() {
        return LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
    }

    private static String request(LocalDate start, int days, String pace, String extra) {
        return "{\"startDate\":\"%s\",\"days\":%d,\"pace\":\"%s\",\"lodging\":\"VINH_LONG_CENTER\",\"adults\":2,\"children\":1%s}"
                .formatted(start, days, pace, extra);
    }

    private ResultActions suggest(String body) throws Exception {
        return mvc.perform(post("/api/v1/itineraries/suggest").contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private static String body(ResultActions r) throws Exception {
        return r.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    @Test
    @Transactional(readOnly = true)
    void planRespectsOpeningHoursClosedDaysLunchAndDayLength() throws Exception {
        LocalDate start = today().plusDays(3);
        String json = body(suggest(request(start, 3, "MODERATE", "")).andExpect(status().isOk()));
        Map<Long, Destination> byId = destinations.findAll().stream().collect(Collectors.toMap(Destination::getId, d -> d));

        List<Map<String, Object>> days = JsonPath.read(json, "$.dayPlans");
        assertThat(days).hasSize(3);
        Set<Object> seen = new HashSet<>();
        for (int d = 0; d < days.size(); d++) {
            LocalDate date = start.plusDays(d);
            assertThat(LocalTime.parse(JsonPath.read(json, "$.dayPlans[%d].returnAt".formatted(d)))).isBeforeOrEqualTo(LocalTime.of(17, 30));
            List<Map<String, Object>> stops = JsonPath.read(json, "$.dayPlans[%d].stops".formatted(d));
            assertThat(stops.stream().filter(s -> s.get("type").equals("LUNCH"))).singleElement()
                    .satisfies(s -> assertThat(LocalTime.parse((String) s.get("start"))).isBetween(LocalTime.of(11, 0), LocalTime.of(13, 30)));
            for (Map<String, Object> s : stops) {
                if (s.get("type").equals("VISIT")) {
                    Destination dest = byId.get(((Number) s.get("destinationId")).longValue());
                    assertThat(seen.add(dest.getId())).as("không ghé một điểm hai lần").isTrue();
                    assertThat(LocalTime.parse((String) s.get("start"))).isAfterOrEqualTo(dest.getOpeningHours().openTime());
                    assertThat(LocalTime.parse((String) s.get("leave"))).isBeforeOrEqualTo(dest.getOpeningHours().closeTime());
                    assertThat(dest.getClosedDays()).doesNotContain(date.getDayOfWeek());
                }
            }
        }
        assertThat(seen).isNotEmpty();
        assertThat((Integer) JsonPath.read(json, "$.totalScore")).isPositive();
    }

    @Test
    void sameRequestGivesSamePlan() throws Exception {
        String r = request(today().plusDays(5), 2, "PACKED", "");

        assertThat(body(suggest(r))).isEqualTo(body(suggest(r)));
    }

    @Test
    void neverPlansAClosedDestination() throws Exception {
        // Văn Thánh Miếu (VL01) đóng cửa Chủ nhật; khách mê lịch sử, ở ngay Vĩnh Long, đi đúng Chủ nhật
        long history = idByName("/api/v1/tour-categories", "Lịch sử");
        LocalDate sunday = today().plusDays(1).with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

        String json = body(suggest(request(sunday, 1, "MODERATE", ",\"categoryIds\":[%d]".formatted(history))).andExpect(status().isOk()));

        List<String> codes = JsonPath.read(json, "$.dayPlans[*].stops[*].destinationCode");
        assertThat(codes).doesNotContain("VL01");
    }

    @Test
    void budgetCapsTheCostAndMustCoverLunch() throws Exception {
        // 3 khách × 80.000 đ tiền ăn trưa × 2 ngày = 480.000 đ
        String json = body(suggest(request(today().plusDays(2), 2, "PACKED", ",\"budget\":600000")).andExpect(status().isOk()));
        assertThat(((Number) JsonPath.read(json, "$.estimatedCost")).longValue()).isBetween(480_000L, 600_000L);

        suggest(request(today().plusDays(2), 2, "PACKED", ",\"budget\":479999"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("BUDGET_TOO_LOW"));
    }

    @Test
    void rejectsBadInput() throws Exception {
        suggest(request(today().minusDays(1), 1, "RELAXED", "")).andExpect(jsonPath("$.code").value("INVALID_START_DATE"));
        suggest("{\"startDate\":\"%s\",\"days\":1,\"pace\":\"RELAXED\",\"adults\":1}".formatted(today()))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("LODGING_REQUIRED"));
        suggest(request(today(), 6, "RELAXED", "")).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        // Tọa độ tự chọn thay cho nơi lưu trú gợi ý sẵn
        suggest("{\"startDate\":\"%s\",\"days\":1,\"pace\":\"RELAXED\",\"adults\":1,\"lodgingLatitude\":9.93,\"lodgingLongitude\":106.34}"
                .formatted(today())).andExpect(status().isOk()).andExpect(jsonPath("$.lodging.name").value("Nơi lưu trú tự chọn"));
    }

    @Test
    void suggestsTheTourCoveringTheSamePlaces() throws Exception {
        String admin = bearer(adminToken(mvc));
        String r = request(today().plusDays(4), 1, "RELAXED", "");
        List<Number> visited = JsonPath.read(body(suggest(r)), "$.dayPlans[0].stops[?(@.type == 'VISIT')].destinationId");
        long departureId = TestTours.bookableDeparture(mvc, admin, 10, 15);
        long tourId = ((Number) JsonPath.read(body(mvc.perform(get("/api/v1/departures/{id}", departureId))), "$.tourId")).longValue();
        String activities = visited.stream().map(id -> "{\"startTime\":\"08:00\",\"title\":\"Tham quan\",\"destinationId\":" + id + "}")
                .collect(Collectors.joining(","));
        mvc.perform(put("/api/v1/admin/tours/{id}/itinerary", tourId).header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                .content("{\"days\":[{\"title\":\"Ngày 1\",\"activities\":[" + activities + "]}]}")).andExpect(status().isOk());

        suggest(r).andExpect(jsonPath("$.similarTours[0].tour.id").value(tourId))
                .andExpect(jsonPath("$.similarTours[0].similarity").value(1.0))
                .andExpect(jsonPath("$.similarTours[0].tour.fromPrice").value(TestTours.ADULT_PRICE));
    }

    @Test
    void customerSavesListsReadsAndDeletesOwnTrips() throws Exception {
        String customer = bearer(customerToken(mvc));
        String save = "{\"title\":\"Đi Vĩnh Long cuối tuần\",\"request\":" + request(today().plusDays(6), 2, "MODERATE", "") + "}";

        mvc.perform(post("/api/v1/itineraries").contentType(MediaType.APPLICATION_JSON).content(save)).andExpect(status().isUnauthorized());
        String saved = body(mvc.perform(post("/api/v1/itineraries").header("Authorization", customer)
                .contentType(MediaType.APPLICATION_JSON).content(save)).andExpect(status().isCreated()));
        long id = ((Number) JsonPath.read(saved, "$.id")).longValue();

        mvc.perform(get("/api/v1/me/itineraries").header("Authorization", customer))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Đi Vĩnh Long cuối tuần"))
                .andExpect(jsonPath("$[0].days").value(2));
        mvc.perform(get("/api/v1/me/itineraries/{id}", id).header("Authorization", customer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plan.totalScore").value((Integer) JsonPath.read(saved, "$.plan.totalScore")))
                .andExpect(jsonPath("$.plan.dayPlans.length()").value(2))
                .andExpect(jsonPath("$.request.pace").value("MODERATE"));

        String other = bearer(customerToken(mvc));
        mvc.perform(get("/api/v1/me/itineraries/{id}", id).header("Authorization", other)).andExpect(status().isNotFound());
        mvc.perform(delete("/api/v1/me/itineraries/{id}", id).header("Authorization", other)).andExpect(status().isNotFound());

        mvc.perform(delete("/api/v1/me/itineraries/{id}", id).header("Authorization", customer)).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/me/itineraries").header("Authorization", customer)).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void lodgingPresetsArePublic() throws Exception {
        mvc.perform(get("/api/v1/itineraries/lodging-presets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[0].code").value("VINH_LONG_CENTER"));
    }

    private long idByName(String url, String name) throws Exception {
        List<Number> ids = JsonPath.read(body(mvc.perform(get(url))), "$[?(@.name == '" + name + "')].id");
        return ids.getFirst().longValue();
    }
}
