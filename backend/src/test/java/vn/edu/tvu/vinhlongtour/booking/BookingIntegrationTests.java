package vn.edu.tvu.vinhlongtour.booking;

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
import vn.edu.tvu.vinhlongtour.booking.service.BookingExpiryJob;
import vn.edu.tvu.vinhlongtour.booking.service.BookingService;
import vn.edu.tvu.vinhlongtour.booking.web.BookingDtos.BookingRequest;
import vn.edu.tvu.vinhlongtour.booking.web.BookingDtos.PassengerInput;
import vn.edu.tvu.vinhlongtour.common.ApiException;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static vn.edu.tvu.vinhlongtour.TestAuth.adminToken;
import static vn.edu.tvu.vinhlongtour.TestAuth.bearer;
import static vn.edu.tvu.vinhlongtour.TestAuth.customerToken;

/**
 * Kiểm tra luồng đặt tour trên MySQL thật: báo giá, giữ chỗ, tra cứu, đơn của tôi,
 * nhiều người tranh chỗ cuối cùng cùng lúc, hết hạn giữ chỗ và admin hủy đơn.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class BookingIntegrationTests {

    private static final String PHONE = "0901234567";

    @Autowired
    MockMvc mvc;
    @Autowired
    BookingService bookingService;
    @Autowired
    BookingExpiryJob expiryJob;

    private String admin;

    @BeforeEach
    void setUp() throws Exception {
        admin = bearer(adminToken(mvc));
    }

    @Test
    void quoteShowsTotalsAndDepositWithoutHoldingSeats() throws Exception {
        long departureId = TestTours.bookableDeparture(mvc, admin, 10, 20);

        mvc.perform(post("/api/v1/bookings/quote").contentType(MediaType.APPLICATION_JSON)
                        .content(quoteJson(departureId, adultAndChild())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAmount").value(TestTours.ADULT_PRICE + TestTours.CHILD_PRICE))
                .andExpect(jsonPath("$.minimumFirstPayment").value(405_000))   // 30% của 1.350.000
                .andExpect(jsonPath("$.passengers[1].passengerTypeName").value("Trẻ em"));
        seatsLeft(departureId, 10);
    }

    @Test
    void guestBooksAndLooksUpWithCodeAndPhone() throws Exception {
        long departureId = TestTours.bookableDeparture(mvc, admin, 10, 20);

        String json = book(departureId, adultAndChild(), null).andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"))
                .andExpect(jsonPath("$.code").value(org.hamcrest.Matchers.matchesPattern("VL\\d{6}[A-Z2-9]{5}")))
                .andExpect(jsonPath("$.seats").value(2))
                .andExpect(jsonPath("$.history[0].toStatus").value("PENDING_PAYMENT"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String code = JsonPath.read(json, "$.code");
        LocalDateTime holdUntil = LocalDateTime.parse(JsonPath.read(json, "$.holdUntil"));
        assertThat(holdUntil).isBetween(now().plusMinutes(29), now().plusMinutes(31));
        seatsLeft(departureId, 8);

        mvc.perform(get("/api/v1/bookings/lookup").param("code", code.toLowerCase()).param("phone", PHONE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passengers.length()").value(2));
        mvc.perform(get("/api/v1/bookings/lookup").param("code", code).param("phone", "0999999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("BOOKING_NOT_FOUND"));
    }

    @Test
    void loggedInBookingAppearsOnlyInOwnersList() throws Exception {
        long departureId = TestTours.bookableDeparture(mvc, admin, 10, 20);
        String owner = bearer(customerToken(mvc));
        String someoneElse = bearer(customerToken(mvc));

        String code = JsonPath.read(book(departureId, adultAndChild(), owner).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.code");

        mvc.perform(get("/api/v1/me/bookings").header("Authorization", owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value(code));
        mvc.perform(get("/api/v1/me/bookings/{code}", code).header("Authorization", someoneElse))
                .andExpect(status().isNotFound());
    }

    @Test
    void bookingRulesAreEnforced() throws Exception {
        long departureId = TestTours.bookableDeparture(mvc, admin, 1, 20);

        book(departureId, adultAndChild(), null).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SEAT_NOT_ENOUGH"));
        book(departureId, "[" + passenger("Bé", LocalDate.now().minusYears(8)) + "]", null).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ADULT_REQUIRED"));

        // Admin đóng đợt: không nhận thêm khách
        mvc.perform(put("/api/v1/admin/departures/{id}", departureId).header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                        .content(TestTours.departureJson(mvc, LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusDays(20).withNano(0), 1, "CLOSED")))
                .andExpect(status().isOk());
        book(departureId, "[" + passenger("Người lớn", LocalDate.of(1990, 1, 1)) + "]", null).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DEPARTURE_NOT_OPEN"));
    }

    @Test
    void manyPeopleRacingForLastSeatsNeverOverbook() throws Exception {
        long departureId = TestTours.bookableDeparture(mvc, admin, 2, 20);
        int people = 8;
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(people);
        List<Future<String>> results = new ArrayList<>();
        for (int i = 0; i < people; i++) {
            BookingRequest r = new BookingRequest(departureId, "Khách " + i, PHONE, null, null,
                    List.of(new PassengerInput("Khách " + i, LocalDate.of(1990, 1, 1), null, null, null, null)), null);
            Callable<String> attempt = () -> {
                start.await();
                try {
                    bookingService.create(r, null);
                    return "OK";
                } catch (ApiException e) {
                    return e.getCode();
                }
            };
            results.add(pool.submit(attempt));
        }
        start.countDown(); // cho cả 8 người bấm "Đặt" cùng lúc
        List<String> outcomes = new ArrayList<>();
        for (Future<String> f : results) {
            outcomes.add(f.get());
        }
        pool.shutdown();

        assertThat(outcomes).filteredOn("OK"::equals).hasSize(2);
        assertThat(outcomes).filteredOn("SEAT_NOT_ENOUGH"::equals).hasSize(6);
        seatsLeft(departureId, 0);
    }

    @Test
    void overdueHoldIsExpiredAndSeatsReturned() throws Exception {
        long departureId = TestTours.bookableDeparture(mvc, admin, 5, 20);
        String code = JsonPath.read(book(departureId, adultAndChild(), null).andReturn().getResponse()
                .getContentAsString(StandardCharsets.UTF_8), "$.code");
        seatsLeft(departureId, 3);

        assertThat(expiryJob.expireOverdueHolds(now().plusMinutes(10))).as("chưa quá hạn").isZero();
        assertThat(expiryJob.expireOverdueHolds(now().plusMinutes(31))).isGreaterThanOrEqualTo(1);

        mvc.perform(get("/api/v1/bookings/lookup").param("code", code).param("phone", PHONE))
                .andExpect(jsonPath("$.status").value("EXPIRED"))
                .andExpect(jsonPath("$.holdUntil").doesNotExist())
                .andExpect(jsonPath("$.history[1].changedBy").doesNotExist());   // hệ thống tự động
        seatsLeft(departureId, 5);
    }

    @Test
    void adminCancelsUnpaidBookingAndCannotCompleteIt() throws Exception {
        long departureId = TestTours.bookableDeparture(mvc, admin, 5, 20);
        String json = book(departureId, adultAndChild(), null).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        long id = ((Number) JsonPath.read(json, "$.id")).longValue();
        String code = JsonPath.read(json, "$.code");

        mvc.perform(get("/api/v1/admin/bookings").param("q", code).header("Authorization", admin))
                .andExpect(jsonPath("$.total").value(1));
        changeStatus(id, "COMPLETED").andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATUS_CHANGE"));
        changeStatus(id, "CANCELLED").andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.history[1].changedBy").value("Quản trị viên"));
        seatsLeft(departureId, 5);
    }

    // ---------- Hàm phụ ----------

    private ResultActions book(long departureId, String passengers, String bearer) throws Exception {
        var request = post("/api/v1/bookings").contentType(MediaType.APPLICATION_JSON).content("""
                {"departureId":%d,"contactName":"Nguyễn Văn A","contactPhone":"%s","contactEmail":"a@test.vn","passengers":%s}
                """.formatted(departureId, PHONE, passengers));
        return mvc.perform(bearer == null ? request : request.header("Authorization", bearer));
    }

    private ResultActions changeStatus(long id, String status) throws Exception {
        return mvc.perform(patch("/api/v1/admin/bookings/{id}/status", id).header("Authorization", admin)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"%s\",\"reason\":\"Khách báo không đi\"}".formatted(status)));
    }

    private void seatsLeft(long departureId, int expected) throws Exception {
        mvc.perform(get("/api/v1/departures/{id}", departureId)).andExpect(jsonPath("$.seatsLeft").value(expected));
    }

    private static String quoteJson(long departureId, String passengers) {
        return "{\"departureId\":%d,\"passengers\":%s}".formatted(departureId, passengers);
    }

    private static String adultAndChild() {
        return "[" + passenger("Nguyễn Văn A", LocalDate.of(1990, 1, 1)) + "," + passenger("Nguyễn Văn Bé", LocalDate.now().minusYears(8)) + "]";
    }

    private static String passenger(String name, LocalDate birthDate) {
        return "{\"fullName\":\"%s\",\"birthDate\":\"%s\"}".formatted(name + " " + UUID.randomUUID().toString().substring(0, 4), birthDate);
    }

    private static LocalDateTime now() {
        return LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
    }
}
