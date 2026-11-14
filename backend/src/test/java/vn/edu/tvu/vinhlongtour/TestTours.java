package vn.edu.tvu.vinhlongtour;

import com.jayway.jsonpath.JsonPath;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Hàm phụ cho test tích hợp: tạo nhanh một tour 1 ngày đang bán và một đợt khởi hành đặt được,
 * với giá người lớn 900.000đ, trẻ em 450.000đ, em bé 0đ.
 */
public final class TestTours {

    public static final long ADULT_PRICE = 900_000;
    public static final long CHILD_PRICE = 450_000;

    private TestTours() {
    }

    /** @return mã đợt khởi hành mới, khởi hành sau {@code daysFromNow} ngày */
    public static long bookableDeparture(MockMvc mvc, String admin, int capacity, int daysFromNow) throws Exception {
        long tourId = id(mvc.perform(post("/api/v1/admin/tours").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                .content(tourJson(mvc, "B" + UUID.randomUUID().toString().substring(0, 6).toUpperCase(), null))));
        mvc.perform(put("/api/v1/admin/tours/{id}/images", tourId).header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                .content("{\"images\":[{\"url\":\"/uploads/a.jpg\"}]}")).andExpect(status().isOk());
        mvc.perform(put("/api/v1/admin/tours/{id}/itinerary", tourId).header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                .content("{\"days\":[{\"title\":\"Cù lao An Bình\",\"activities\":[]}]}")).andExpect(status().isOk());
        String code = JsonPath.read(content(mvc.perform(get("/api/v1/admin/tours/{id}", tourId).header("Authorization", admin))), "$.code");
        mvc.perform(put("/api/v1/admin/tours/{id}", tourId).header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                .content(tourJson(mvc, code, "ON_SALE"))).andExpect(status().isOk());

        LocalDateTime start = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusDays(daysFromNow).truncatedTo(ChronoUnit.HOURS);
        return id(mvc.perform(post("/api/v1/admin/tours/{id}/departures", tourId).header("Authorization", admin)
                .contentType(MediaType.APPLICATION_JSON).content(departureJson(mvc, start, capacity, null))));
    }

    public static String departureJson(MockMvc mvc, LocalDateTime start, int capacity, String status) throws Exception {
        List<Number> ids = JsonPath.read(content(mvc.perform(get("/api/v1/passenger-types"))), "$[*].id");
        long[] prices = {ADULT_PRICE, CHILD_PRICE, 0};
        String priceJson = IntStream.range(0, ids.size())
                .mapToObj(i -> "{\"passengerTypeId\":%d,\"price\":%d}".formatted(ids.get(i).longValue(), prices[Math.min(i, 2)]))
                .collect(Collectors.joining(",", "[", "]"));
        return "{\"departureAt\":\"%s\",\"returnAt\":\"%s\",\"bookingDeadline\":\"%s\",\"capacity\":%d,\"prices\":%s%s}"
                .formatted(start, start.plusHours(10), start.minusDays(1), capacity, priceJson,
                        status == null ? "" : ",\"status\":\"" + status + "\"");
    }

    private static String tourJson(MockMvc mvc, String code, String status) throws Exception {
        long region = idByName(mvc, "/api/v1/regions", "Vĩnh Long (cũ)");
        long category = idByName(mvc, "/api/v1/tour-categories", "Sinh thái");
        return "{\"code\":\"%s\",\"name\":\"Tour đặt thử %s\",\"durationDays\":1,\"durationNights\":0,\"regionIds\":[%d],\"categoryIds\":[%d]%s}"
                .formatted(code, code, region, category, status == null ? "" : ",\"status\":\"" + status + "\"");
    }

    private static long idByName(MockMvc mvc, String url, String name) throws Exception {
        List<Number> ids = JsonPath.read(content(mvc.perform(get(url))), "$[?(@.name == '" + name + "')].id");
        return ids.getFirst().longValue();
    }

    private static long id(org.springframework.test.web.servlet.ResultActions result) throws Exception {
        return ((Number) JsonPath.read(content(result.andExpect(status().isCreated())), "$.id")).longValue();
    }

    private static String content(org.springframework.test.web.servlet.ResultActions result) throws Exception {
        return result.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }
}
