package vn.edu.tvu.vinhlongtour.payment;

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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import vn.edu.tvu.vinhlongtour.TestTours;
import vn.edu.tvu.vinhlongtour.TestcontainersConfiguration;
import vn.edu.tvu.vinhlongtour.booking.service.BookingExpiryJob;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static vn.edu.tvu.vinhlongtour.TestAuth.adminToken;
import static vn.edu.tvu.vinhlongtour.TestAuth.bearer;

/**
 * Kiểm tra thanh toán trên PostgreSQL thật, giả lập VNPay bằng cách tự ký dữ liệu IPN với khóa test:
 * chống ghi trùng, chặn sai chữ ký/sai số tiền, trả thừa, trả sau khi hết hạn giữ chỗ, xác nhận thủ công.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class PaymentIntegrationTests {

    private static final String PHONE = "0907654321";
    private static final long TOTAL = TestTours.ADULT_PRICE;           // mỗi đơn 1 người lớn
    private static final long DEPOSIT = 270_000;                        // 30% của 900.000

    @Autowired
    MockMvc mvc;
    @Autowired
    VnPayGateway gateway;
    @Autowired
    BookingExpiryJob expiryJob;

    private String admin;

    @BeforeEach
    void setUp() throws Exception {
        admin = bearer(adminToken(mvc));
    }

    @Test
    void depositViaVnPayIsRecordedOnceEvenIfIpnRepeats() throws Exception {
        Booking b = book(TestTours.bookableDeparture(mvc, admin, 5, 20));
        String ref = link(b.code(), "DEPOSIT").andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(DEPOSIT))
                .andExpect(jsonPath("$.paymentUrl").value(org.hamcrest.Matchers.containsString("vnp_Amount=" + DEPOSIT * 100)))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8).transform(j -> JsonPath.read(j, "$.reference"));

        ipn(ref, DEPOSIT, "00").andExpect(jsonPath("$.RspCode").value("00"));
        ipn(ref, DEPOSIT, "00").andExpect(jsonPath("$.RspCode").value("02"));   // VNPay gửi lại: không cộng lần nữa

        lookup(b.code()).andExpect(jsonPath("$.status").value("DEPOSITED"))
                .andExpect(jsonPath("$.paidAmount").value(DEPOSIT))
                .andExpect(jsonPath("$.payments[0].status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.payments[0].transactionNo").value("VNP" + ref.hashCode()));
    }

    @Test
    void forgedOrWrongCallbacksAreRejected() throws Exception {
        Booking b = book(TestTours.bookableDeparture(mvc, admin, 5, 20));
        String ref = reference(link(b.code(), "FULL"));

        Map<String, String> forged = callback(ref, TOTAL, "00");
        forged.put("vnp_SecureHash", "abc123");
        mvc.perform(withParams(get("/api/v1/payments/vnpay/ipn"), forged)).andExpect(jsonPath("$.RspCode").value("97"));
        ipn(ref, TOTAL - 1000, "00").andExpect(jsonPath("$.RspCode").value("04"));
        ipn("KHONG-CO-MA-NAY", TOTAL, "00").andExpect(jsonPath("$.RspCode").value("01"));

        lookup(b.code()).andExpect(jsonPath("$.paidAmount").value(0)).andExpect(jsonPath("$.status").value("PENDING_PAYMENT"));
    }

    @Test
    void onlyOneVnPayLinkWaitsAtATimeAndFailedPaymentFreesIt() throws Exception {
        Booking b = book(TestTours.bookableDeparture(mvc, admin, 5, 20));
        String ref = reference(link(b.code(), "FULL"));

        link(b.code(), "FULL").andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("PAYMENT_IN_PROGRESS"));

        ipn(ref, TOTAL, "24").andExpect(jsonPath("$.RspCode").value("00"));   // khách bấm hủy trên VNPay
        lookup(b.code()).andExpect(jsonPath("$.payments[0].status").value("FAILED")).andExpect(jsonPath("$.paidAmount").value(0));
        link(b.code(), "FULL").andExpect(status().isCreated());
    }

    @Test
    void returnPageRecordsPaymentWhenIpnCannotReachLocalhost() throws Exception {
        Booking b = book(TestTours.bookableDeparture(mvc, admin, 5, 20));
        String ref = reference(link(b.code(), "FULL"));

        mvc.perform(withParams(get("/api/v1/payments/vnpay/return"), callback(ref, TOTAL, "00")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.validSignature").value(true))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.bookingCode").value(b.code()));
        lookup(b.code()).andExpect(jsonPath("$.status").value("PAID"));

        // Dữ liệu có chữ ký đúng nhưng số tiền không khớp: không được báo "thành công" cho khách
        mvc.perform(withParams(get("/api/v1/payments/vnpay/return"), callback(ref, TOTAL - 1000, "00")))
                .andExpect(jsonPath("$.validSignature").value(true))
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void manualPaymentIsCappedAndOverpaymentIsFlaggedForRefund() throws Exception {
        Booking b = book(TestTours.bookableDeparture(mvc, admin, 5, 20));
        String ref = reference(link(b.code(), "FULL"));

        manual(b.id(), TOTAL + 1).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("AMOUNT_EXCEEDS_REMAINING"));
        manual(b.id(), TOTAL).andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.payments[1].confirmedBy").value("Quản trị viên"));

        // Khách vẫn trả nốt link VNPay cũ: tiền vẫn được ghi nhận, đơn hiện "cần hoàn"
        ipn(ref, TOTAL, "00").andExpect(jsonPath("$.RspCode").value("00"));
        lookup(b.code()).andExpect(jsonPath("$.refundDue").value(TOTAL));
        mvc.perform(get("/api/v1/admin/bookings").param("needsRefund", "true").param("size", "100").header("Authorization", admin))
                .andExpect(jsonPath("$.items[*].code", hasItem(b.code())));
    }

    @Test
    void latePaymentRevivesExpiredBookingWhenSeatsRemain() throws Exception {
        long departureId = TestTours.bookableDeparture(mvc, admin, 5, 20);
        Booking b = book(departureId);
        String ref = reference(link(b.code(), "DEPOSIT"));
        expiryJob.expireOverdueHolds(now().plusMinutes(31));
        lookup(b.code()).andExpect(jsonPath("$.status").value("EXPIRED"));

        ipn(ref, DEPOSIT, "00").andExpect(jsonPath("$.RspCode").value("00"));

        lookup(b.code()).andExpect(jsonPath("$.status").value("DEPOSITED")).andExpect(jsonPath("$.refundDue").value(0));
        mvc.perform(get("/api/v1/departures/{id}", departureId)).andExpect(jsonPath("$.seatsLeft").value(4));
    }

    @Test
    void latePaymentForExpiredBookingWithoutSeatsMustBeRefunded() throws Exception {
        long departureId = TestTours.bookableDeparture(mvc, admin, 1, 20);
        Booking late = book(departureId);
        String ref = reference(link(late.code(), "DEPOSIT"));
        expiryJob.expireOverdueHolds(now().plusMinutes(31));
        book(departureId);                                                       // người khác lấy mất chỗ duy nhất

        ipn(ref, DEPOSIT, "00").andExpect(jsonPath("$.RspCode").value("00"));

        lookup(late.code()).andExpect(jsonPath("$.status").value("EXPIRED")).andExpect(jsonPath("$.refundDue").value(DEPOSIT));
        manual(late.id(), 1000).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("NO_SEATS_TO_REVIVE"));
    }

    @Test
    void strangerCannotCreatePaymentLink() throws Exception {
        Booking b = book(TestTours.bookableDeparture(mvc, admin, 5, 20));
        mvc.perform(post("/api/v1/payments/vnpay").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookingCode\":\"%s\",\"phone\":\"0999999999\",\"type\":\"FULL\"}".formatted(b.code())))
                .andExpect(status().isNotFound());
    }

    // ---------- Hàm phụ ----------

    private record Booking(long id, String code) {}

    private Booking book(long departureId) throws Exception {
        String json = mvc.perform(post("/api/v1/bookings").contentType(MediaType.APPLICATION_JSON).content("""
                        {"departureId":%d,"contactName":"Lê Văn D","contactPhone":"%s",
                         "passengers":[{"fullName":"Lê Văn D","birthDate":"%s"}]}
                        """.formatted(departureId, PHONE, LocalDate.of(1988, 8, 8))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return new Booking(((Number) JsonPath.read(json, "$.id")).longValue(), JsonPath.read(json, "$.code"));
    }

    private ResultActions link(String code, String type) throws Exception {
        return mvc.perform(post("/api/v1/payments/vnpay").contentType(MediaType.APPLICATION_JSON)
                .content("{\"bookingCode\":\"%s\",\"phone\":\"%s\",\"type\":\"%s\"}".formatted(code, PHONE, type)));
    }

    private static String reference(ResultActions linkResult) throws Exception {
        return JsonPath.read(linkResult.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8),
                "$.reference");
    }

    /** Dữ liệu VNPay gửi về, ký bằng khóa test giống như VNPay ký bằng khóa thật. */
    private Map<String, String> callback(String ref, long amount, String responseCode) {
        Map<String, String> p = new HashMap<>();
        p.put("vnp_TmnCode", "TESTTMN1");
        p.put("vnp_TxnRef", ref);
        p.put("vnp_Amount", String.valueOf(amount * 100));
        p.put("vnp_ResponseCode", responseCode);
        p.put("vnp_TransactionStatus", "00".equals(responseCode) ? "00" : "02");
        p.put("vnp_TransactionNo", "VNP" + ref.hashCode());
        p.put("vnp_BankCode", "NCB");
        p.put("vnp_PayDate", "20261114103000");
        p.put("vnp_OrderInfo", "Thanh toan don");
        p.put("vnp_SecureHash", gateway.sign(p));
        return p;
    }

    private ResultActions ipn(String ref, long amount, String responseCode) throws Exception {
        return mvc.perform(withParams(get("/api/v1/payments/vnpay/ipn"), callback(ref, amount, responseCode)));
    }

    private ResultActions manual(long bookingId, long amount) throws Exception {
        return mvc.perform(post("/api/v1/admin/bookings/{id}/payments", bookingId).header("Authorization", admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\":%d,\"method\":\"BANK_TRANSFER\",\"transactionNo\":\"CK001\",\"proofUrl\":\"/uploads/ck.jpg\"}".formatted(amount)));
    }

    private ResultActions lookup(String code) throws Exception {
        return mvc.perform(get("/api/v1/bookings/lookup").param("code", code).param("phone", PHONE));
    }

    private static MockHttpServletRequestBuilder withParams(MockHttpServletRequestBuilder request, Map<String, String> params) {
        params.forEach(request::param);
        return request;
    }

    private static LocalDateTime now() {
        return LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
    }
}
