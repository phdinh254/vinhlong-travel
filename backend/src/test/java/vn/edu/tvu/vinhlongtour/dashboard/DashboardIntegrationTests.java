package vn.edu.tvu.vinhlongtour.dashboard;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import vn.edu.tvu.vinhlongtour.TestTours;
import vn.edu.tvu.vinhlongtour.TestcontainersConfiguration;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static vn.edu.tvu.vinhlongtour.TestAuth.adminToken;
import static vn.edu.tvu.vinhlongtour.TestAuth.bearer;
import static vn.edu.tvu.vinhlongtour.TestAuth.customerToken;

/**
 * Kiểm tra dashboard đếm đúng: đi một vòng đặt → trả tiền → xin hủy → duyệt → hoàn một phần, rồi so số liệu
 * trước và sau (CSDL test dùng chung với các test khác nên so chênh lệch thay vì số tuyệt đối).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class DashboardIntegrationTests {

    private static final String PHONE = "0911222333";

    @Autowired
    MockMvc mvc;

    private DocumentContext dashboard(String admin) throws Exception {
        return JsonPath.parse(mvc.perform(get("/api/v1/admin/dashboard").header("Authorization", admin))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private static long n(DocumentContext d, String path) {
        return d.read(path, Long.class);
    }

    private String post(String url, String admin, String body, int expected) throws Exception {
        var request = org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(url).contentType(MediaType.APPLICATION_JSON).content(body);
        if (admin != null) {
            request.header("Authorization", admin);
        }
        return mvc.perform(request).andExpect(status().is(expected)).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    @Test
    void countsMoneyBookingsAndPendingWork() throws Exception {
        String admin = bearer(adminToken(mvc));
        DocumentContext before = dashboard(admin);

        long departureId = TestTours.bookableDeparture(mvc, admin, 5, 20);
        String booking = post("/api/v1/bookings", null, """
                {"departureId":%d,"contactName":"Võ Thị N","contactPhone":"%s","passengers":[{"fullName":"Võ Thị N","birthDate":"1990-01-01"}]}
                """.formatted(departureId, PHONE), 201);
        long bookingId = ((Number) JsonPath.read(booking, "$.id")).longValue();
        String code = JsonPath.read(booking, "$.code");
        post("/api/v1/admin/bookings/" + bookingId + "/payments", admin,
                "{\"amount\":%d,\"method\":\"CASH\"}".formatted(TestTours.ADULT_PRICE), 201);
        String requested = post("/api/v1/bookings/" + code + "/cancellation-requests", null,
                "{\"scope\":\"FULL\",\"phone\":\"%s\"}".formatted(PHONE), 201);
        post("/api/v1/contact-messages", null, "{\"fullName\":\"Võ Thị N\",\"phone\":\"%s\",\"message\":\"Hỏi tour\"}".formatted(PHONE), 201);

        DocumentContext middle = dashboard(admin);
        assertThat(n(middle, "$.newBookings") - n(before, "$.newBookings")).isEqualTo(1);
        assertThat(n(middle, "$.bookingsByStatus.PAID") - n(before, "$.bookingsByStatus.PAID")).isEqualTo(1);
        assertThat(n(middle, "$.collected") - n(before, "$.collected")).isEqualTo(TestTours.ADULT_PRICE);
        assertThat(n(middle, "$.pendingCancellationRequests") - n(before, "$.pendingCancellationRequests")).isEqualTo(1);
        assertThat(n(middle, "$.newContactMessages") - n(before, "$.newContactMessages")).isEqualTo(1);
        assertThat(n(middle, "$.toursOnSale") - n(before, "$.toursOnSale")).isEqualTo(1);

        long requestId = ((Number) JsonPath.read(requested, "$.cancellationRequests[0].id")).longValue();
        long refundDue = ((Number) JsonPath.read(post("/api/v1/admin/cancellation-requests/" + requestId + "/approve", admin, "{}", 200),
                "$.refundDue")).longValue();
        assertThat(refundDue).isGreaterThan(100_000);
        post("/api/v1/admin/bookings/" + bookingId + "/refunds", admin, "{\"amount\":100000,\"method\":\"BANK_TRANSFER\"}", 201);

        DocumentContext after = dashboard(admin);
        assertThat(n(after, "$.bookingsByStatus.PAID") - n(before, "$.bookingsByStatus.PAID")).isZero();
        assertThat(n(after, "$.bookingsByStatus.CANCELLED") - n(before, "$.bookingsByStatus.CANCELLED")).isEqualTo(1);
        assertThat(n(after, "$.refunded") - n(before, "$.refunded")).isEqualTo(100_000);
        assertThat(n(after, "$.netRevenue")).isEqualTo(n(after, "$.collected") - n(after, "$.refunded"));
        assertThat(n(after, "$.pendingCancellationRequests")).isEqualTo(n(before, "$.pendingCancellationRequests"));
        // Còn nợ khách (refundDue − 100.000) nên đơn nằm trong danh sách cần hoàn
        assertThat(n(after, "$.bookingsNeedingRefund") - n(before, "$.bookingsNeedingRefund")).isEqualTo(1);
        assertThat(after.read("$.bookingsNeedingRefundList[*].code", java.util.List.class)).contains(code);
        assertThat(after.read("$.recentBookings[0].code", String.class)).isEqualTo(code);
    }

    @Test
    void dateRangeAndAccess() throws Exception {
        String admin = bearer(adminToken(mvc));
        mvc.perform(get("/api/v1/admin/dashboard").param("from", "2026-02-01").param("to", "2026-01-01").header("Authorization", admin))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_DATE_RANGE"));
        mvc.perform(get("/api/v1/admin/dashboard").param("from", "2020-01-01").param("to", "2020-01-31").header("Authorization", admin))
                .andExpect(jsonPath("$.collected").value(0))
                .andExpect(jsonPath("$.newBookings").value(0))
                .andExpect(jsonPath("$.bookingsByStatus.EXPIRED").value(0));
        mvc.perform(get("/api/v1/admin/dashboard").header("Authorization", bearer(customerToken(mvc)))).andExpect(status().isForbidden());
    }
}
