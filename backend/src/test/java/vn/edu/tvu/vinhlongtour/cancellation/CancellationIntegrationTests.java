package vn.edu.tvu.vinhlongtour.cancellation;

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
import vn.edu.tvu.vinhlongtour.TestTours;
import vn.edu.tvu.vinhlongtour.TestcontainersConfiguration;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static vn.edu.tvu.vinhlongtour.TestAuth.adminToken;
import static vn.edu.tvu.vinhlongtour.TestAuth.bearer;
import static vn.edu.tvu.vinhlongtour.TestAuth.customerToken;

/**
 * Kiểm tra hủy tour, hoàn tiền, chuyển đợt trên PostgreSQL thật: tự duyệt đơn chưa trả tiền,
 * phí phạt theo chính sách đã chốt, hủy một phần, hoàn tiền nhiều lần, chuyển đợt tính lại giá theo tuổi.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class CancellationIntegrationTests {

    private static final String PHONE = "0912345678";

    @Autowired
    MockMvc mvc;

    private String admin;

    @BeforeEach
    void setUp() throws Exception {
        admin = bearer(adminToken(mvc));
        // Chính sách mới nhất, áp dụng từ hôm nay: còn 15–60 ngày thì phạt 10%, sát ngày thì phạt 100%
        mvc.perform(post("/api/v1/admin/cancellation-policies").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Chính sách thử hủy","effectiveFrom":"%s","tiers":[
                                  {"daysBeforeFrom":0,"daysBeforeTo":14,"penaltyPercent":100},
                                  {"daysBeforeFrom":15,"daysBeforeTo":60,"penaltyPercent":10}]}
                                """.formatted(today())))
                .andExpect(status().isCreated());
    }

    @Test
    void unpaidBookingIsCancelledImmediatelyAndSeatsReturned() throws Exception {
        long departureId = TestTours.bookableDeparture(mvc, admin, 5, 20);
        Booking b = book(departureId, adult("A"));

        cancel(b.code(), "{\"scope\":\"FULL\",\"phone\":\"%s\"}".formatted(PHONE)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancellationRequests[0].status").value("APPROVED"))
                .andExpect(jsonPath("$.refundDue").value(0));
        seatsLeft(departureId, 5);
    }

    @Test
    void paidBookingCancellationWaitsForAdminThenRefundIsCapped() throws Exception {
        long departureId = TestTours.bookableDeparture(mvc, admin, 5, 20);    // còn 20 ngày: phạt 10%
        Booking b = book(departureId, adult("A"));
        pay(b.id(), TestTours.ADULT_PRICE);

        String json = cancel(b.code(), "{\"scope\":\"FULL\",\"reason\":\"Ốm\",\"phone\":\"%s\"}".formatted(PHONE))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.cancellationRequests[0].penaltyPercent").value(10))
                .andExpect(jsonPath("$.cancellationRequests[0].penaltyAmount").value(90_000))
                .andExpect(jsonPath("$.cancellationRequests[0].estimatedRefund").value(810_000))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        long requestId = ((Number) JsonPath.read(json, "$.cancellationRequests[0].id")).longValue();

        mvc.perform(get("/api/v1/admin/cancellation-requests").param("status", "PENDING").param("size", "100").header("Authorization", admin))
                .andExpect(jsonPath("$.items[?(@.id == " + requestId + ")].bookingCode").value(b.code()));
        mvc.perform(post("/api/v1/admin/cancellation-requests/{id}/approve", requestId).header("Authorization", bearer(customerToken(mvc))))
                .andExpect(status().isForbidden());

        approve(requestId).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancellationFee").value(90_000))
                .andExpect(jsonPath("$.refundDue").value(810_000));
        approve(requestId).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("REQUEST_ALREADY_DECIDED"));
        seatsLeft(departureId, 5);                                               // trả chỗ đúng một lần

        refund(b.id(), 810_001).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("REFUND_EXCEEDS_DUE"));
        refund(b.id(), 500_000).andExpect(status().isCreated());
        refund(b.id(), 310_000).andExpect(status().isCreated())
                .andExpect(jsonPath("$.refundDue").value(0))
                .andExpect(jsonPath("$.paidAmount").value(90_000))
                .andExpect(jsonPath("$.refunds.length()").value(2));
    }

    @Test
    void partialCancellationReleasesOnlyThosePassengersSeats() throws Exception {
        long departureId = TestTours.bookableDeparture(mvc, admin, 5, 20);
        Booking b = book(departureId, adult("A") + "," + adult("B"));
        pay(b.id(), 2 * TestTours.ADULT_PRICE);
        List<Number> passengerIds = JsonPath.read(lookupJson(b.code()), "$.passengers[*].id");

        String json = cancel(b.code(), "{\"scope\":\"PARTIAL\",\"passengerIds\":[%d],\"phone\":\"%s\"}"
                .formatted(passengerIds.get(1).longValue(), PHONE)).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        approve(((Number) JsonPath.read(json, "$.cancellationRequests[0].id")).longValue()).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.seats").value(1))
                .andExpect(jsonPath("$.totalAmount").value(900_000 + 90_000))
                .andExpect(jsonPath("$.refundDue").value(810_000))
                .andExpect(jsonPath("$.passengers[1].status").value("CANCELLED"));
        seatsLeft(departureId, 4);
    }

    @Test
    void transferRepricesByAgeOnNewDateAndMovesSeats() throws Exception {
        long fromId = TestTours.bookableDeparture(mvc, admin, 5, 20);
        long tourId = ((Number) JsonPath.read(mvc.perform(get("/api/v1/departures/{id}", fromId)).andReturn().getResponse()
                .getContentAsString(StandardCharsets.UTF_8), "$.tourId")).longValue();
        LocalDateTime laterStart = now().plusDays(40).truncatedTo(ChronoUnit.HOURS);
        String toJson = mvc.perform(post("/api/v1/admin/tours/{id}/departures", tourId).header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content(TestTours.departureJson(mvc, laterStart, 5, null)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        long toId = ((Number) JsonPath.read(toJson, "$.id")).longValue();

        // Bé tròn 12 tuổi giữa hai ngày đi: đợt cũ tính giá trẻ em, đợt mới tính giá người lớn
        LocalDate birthday = now().plusDays(20).toLocalDate().minusYears(12).plusDays(10);
        Booking b = book(fromId, adult("Mẹ") + ",{\"fullName\":\"Bé\",\"birthDate\":\"%s\"}".formatted(birthday));
        pay(b.id(), TestTours.ADULT_PRICE + TestTours.CHILD_PRICE);

        mvc.perform(post("/api/v1/admin/bookings/{id}/transfer", b.id()).header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"departureId\":%d,\"reason\":\"Khách xin đổi ngày\"}".formatted(toId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.departure.id").value(toId))
                .andExpect(jsonPath("$.passengers[1].passengerTypeName").value("Người lớn"))
                .andExpect(jsonPath("$.transfers[0].priceDifference").value(TestTours.ADULT_PRICE - TestTours.CHILD_PRICE))
                .andExpect(jsonPath("$.status").value("DEPOSITED"))
                .andExpect(jsonPath("$.remainingAmount").value(TestTours.ADULT_PRICE - TestTours.CHILD_PRICE));
        seatsLeft(fromId, 5);
        seatsLeft(toId, 3);

        long otherTourDeparture = TestTours.bookableDeparture(mvc, admin, 5, 25);
        mvc.perform(post("/api/v1/admin/bookings/{id}/transfer", b.id()).header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"departureId\":%d}".formatted(otherTourDeparture)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("DIFFERENT_TOUR"));
    }

    // ---------- Hàm phụ ----------

    private record Booking(long id, String code) {}

    private Booking book(long departureId, String passengers) throws Exception {
        String json = mvc.perform(post("/api/v1/bookings").contentType(MediaType.APPLICATION_JSON).content("""
                        {"departureId":%d,"contactName":"Phạm Thị E","contactPhone":"%s","passengers":[%s]}
                        """.formatted(departureId, PHONE, passengers)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return new Booking(((Number) JsonPath.read(json, "$.id")).longValue(), JsonPath.read(json, "$.code"));
    }

    private static String adult(String name) {
        return "{\"fullName\":\"%s\",\"birthDate\":\"1985-05-05\"}".formatted(name);
    }

    private void pay(long bookingId, long amount) throws Exception {
        mvc.perform(post("/api/v1/admin/bookings/{id}/payments", bookingId).header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":%d,\"method\":\"CASH\"}".formatted(amount)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("PAID"));
    }

    private ResultActions cancel(String code, String body) throws Exception {
        return mvc.perform(post("/api/v1/bookings/{code}/cancellation-requests", code).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions approve(long requestId) throws Exception {
        return mvc.perform(post("/api/v1/admin/cancellation-requests/{id}/approve", requestId).header("Authorization", admin)
                .contentType(MediaType.APPLICATION_JSON).content("{\"note\":\"Đồng ý\"}"));
    }

    private ResultActions refund(long bookingId, long amount) throws Exception {
        return mvc.perform(post("/api/v1/admin/bookings/{id}/refunds", bookingId).header("Authorization", admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\":%d,\"method\":\"BANK_TRANSFER\",\"transactionNo\":\"HT%d\"}".formatted(amount, amount)));
    }

    private String lookupJson(String code) throws Exception {
        return mvc.perform(get("/api/v1/bookings/lookup").param("code", code).param("phone", PHONE))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private void seatsLeft(long departureId, int expected) throws Exception {
        mvc.perform(get("/api/v1/departures/{id}", departureId)).andExpect(jsonPath("$.seatsLeft").value(expected));
    }

    private static LocalDateTime now() {
        return LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
    }

    private static LocalDate today() {
        return now().toLocalDate();
    }
}
