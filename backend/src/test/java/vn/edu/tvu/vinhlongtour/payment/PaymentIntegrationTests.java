package vn.edu.tvu.vinhlongtour.payment;

import com.jayway.jsonpath.JsonPath;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.json.JsonMapper;
import vn.edu.tvu.vinhlongtour.TestTours;
import vn.edu.tvu.vinhlongtour.TestcontainersConfiguration;
import vn.edu.tvu.vinhlongtour.booking.service.BookingExpiryJob;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static vn.edu.tvu.vinhlongtour.TestAuth.adminToken;
import static vn.edu.tvu.vinhlongtour.TestAuth.bearer;

/**
 * Kiểm tra thanh toán trên MySQL thật với một payOS giả chạy trong test (nhận yêu cầu tạo link, trả trạng thái link),
 * webhook được ký bằng khóa test như payOS ký bằng khóa thật: chống ghi trùng, chặn sai chữ ký/sai số tiền,
 * trả thừa, trả sau khi hết hạn giữ chỗ, xác nhận thủ công.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class PaymentIntegrationTests {

    private static final String PHONE = "0907654321";
    private static final long TOTAL = TestTours.ADULT_PRICE;           // mỗi đơn 1 người lớn
    private static final long DEPOSIT = 270_000;                        // 30% của 900.000
    private static final String CHECKSUM_KEY = "KHOA-CHECKSUM-PAYOS-CHI-DUNG-CHO-KIEM-THU";   // khớp application-test.yml

    /** Các link payOS giả đang giữ, theo orderCode: số tiền và trạng thái (test đổi trạng thái để giả lập khách trả tiền). */
    private static final Map<Long, Map<String, Object>> LINKS = new ConcurrentHashMap<>();
    private static final HttpServer PAYOS = fakePayOs();

    @DynamicPropertySource
    static void payOsUrl(DynamicPropertyRegistry registry) {
        registry.add("app.payos.api-url", () -> "http://127.0.0.1:" + PAYOS.getAddress().getPort());
    }

    @AfterAll
    static void stopPayOs() {
        PAYOS.stop(0);
    }

    @Autowired
    MockMvc mvc;
    @Autowired
    PayOsGateway gateway;
    @Autowired
    BookingExpiryJob expiryJob;

    private String admin;

    @BeforeEach
    void setUp() throws Exception {
        admin = bearer(adminToken(mvc));
    }

    @Test
    void depositViaPayOsIsRecordedOnceEvenIfWebhookRepeats() throws Exception {
        Booking b = book(TestTours.bookableDeparture(mvc, admin, 5, 20));
        String ref = reference(link(b.code(), "DEPOSIT")
                .andExpect(jsonPath("$.amount").value(DEPOSIT))
                .andExpect(jsonPath("$.checkoutUrl").value("https://pay.payos.vn/web/test"))
                .andExpect(jsonPath("$.qrCode").exists()));
        org.assertj.core.api.Assertions.assertThat(LINKS.get(Long.parseLong(ref))).containsEntry("amount", (int) DEPOSIT);

        webhook(ref, DEPOSIT).andExpect(jsonPath("$.result").value("CONFIRMED"));
        webhook(ref, DEPOSIT).andExpect(jsonPath("$.result").value("ALREADY_CONFIRMED"));   // payOS gửi lại: không cộng lần nữa

        lookup(b.code()).andExpect(jsonPath("$.status").value("DEPOSITED"))
                .andExpect(jsonPath("$.paidAmount").value(DEPOSIT))
                .andExpect(jsonPath("$.payments[0].method").value("PAYOS"))
                .andExpect(jsonPath("$.payments[0].status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.payments[0].transactionNo").value("FT" + ref));
    }

    @Test
    void forgedOrWrongCallbacksAreRejected() throws Exception {
        Booking b = book(TestTours.bookableDeparture(mvc, admin, 5, 20));
        String ref = reference(link(b.code(), "FULL"));

        Map<String, Object> forged = Map.of("data", webhookData(ref, TOTAL), "signature", "abc123");
        postWebhook(forged).andExpect(status().isOk()).andExpect(jsonPath("$.result").value("INVALID_SIGNATURE"));
        postWebhook(Map.of("code", "00")).andExpect(status().isOk());                           // bản thử khi khai báo webhook
        webhook(ref, TOTAL - 1000).andExpect(jsonPath("$.result").value("INVALID_AMOUNT"));
        webhook("123", TOTAL).andExpect(jsonPath("$.result").value("NOT_FOUND"));

        lookup(b.code()).andExpect(jsonPath("$.paidAmount").value(0)).andExpect(jsonPath("$.status").value("PENDING_PAYMENT"));
    }

    @Test
    void onlyOneLinkWaitsAtATimeAndCancelledPaymentFreesIt() throws Exception {
        Booking b = book(TestTours.bookableDeparture(mvc, admin, 5, 20));
        String ref = reference(link(b.code(), "FULL"));

        link(b.code(), "FULL").andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("PAYMENT_IN_PROGRESS"));

        payOsMarks(ref, "CANCELLED");                                                            // khách bấm hủy trên payOS
        returnPage(ref).andExpect(jsonPath("$.paid").value(false)).andExpect(jsonPath("$.status").value("CANCELLED"));
        lookup(b.code()).andExpect(jsonPath("$.payments[0].status").value("FAILED")).andExpect(jsonPath("$.paidAmount").value(0));
        link(b.code(), "FULL").andExpect(status().isCreated());
    }

    @Test
    void returnPageAsksPayOsInsteadOfTrustingTheUrl() throws Exception {
        Booking b = book(TestTours.bookableDeparture(mvc, admin, 5, 20));
        String ref = reference(link(b.code(), "FULL"));

        // Khách tự gõ đường dẫn kết quả khi chưa trả: payOS vẫn báo chờ nên không ghi nhận
        returnPage(ref).andExpect(jsonPath("$.paid").value(false)).andExpect(jsonPath("$.status").value("PENDING"));
        lookup(b.code()).andExpect(jsonPath("$.status").value("PENDING_PAYMENT"));

        payOsMarks(ref, "PAID");
        returnPage(ref).andExpect(status().isOk())
                .andExpect(jsonPath("$.paid").value(true))
                .andExpect(jsonPath("$.bookingCode").value(b.code()))
                .andExpect(jsonPath("$.transactionNo").value("FT" + ref));
        lookup(b.code()).andExpect(jsonPath("$.status").value("PAID")).andExpect(jsonPath("$.paidAmount").value(TOTAL));

        returnPage(ref).andExpect(jsonPath("$.paid").value(true));                              // tải lại trang: không cộng lần nữa
        lookup(b.code()).andExpect(jsonPath("$.paidAmount").value(TOTAL));
        returnPage("abc").andExpect(jsonPath("$.paid").value(false)).andExpect(jsonPath("$.bookingCode").doesNotExist());
    }

    @Test
    void manualPaymentIsCappedAndOverpaymentIsFlaggedForRefund() throws Exception {
        Booking b = book(TestTours.bookableDeparture(mvc, admin, 5, 20));
        String ref = reference(link(b.code(), "FULL"));

        manual(b.id(), TOTAL + 1).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("AMOUNT_EXCEEDS_REMAINING"));
        manual(b.id(), TOTAL).andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.payments[1].confirmedBy").value("Quản trị viên"));

        // Khách vẫn trả nốt link payOS cũ: tiền vẫn được ghi nhận, đơn hiện "cần hoàn"
        webhook(ref, TOTAL).andExpect(jsonPath("$.result").value("CONFIRMED"));
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

        webhook(ref, DEPOSIT).andExpect(jsonPath("$.result").value("CONFIRMED"));

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

        webhook(ref, DEPOSIT).andExpect(jsonPath("$.result").value("CONFIRMED"));

        lookup(late.code()).andExpect(jsonPath("$.status").value("EXPIRED")).andExpect(jsonPath("$.refundDue").value(DEPOSIT));
        manual(late.id(), 1000).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("NO_SEATS_TO_REVIVE"));
    }

    @Test
    void strangerCannotCreatePaymentLink() throws Exception {
        Booking b = book(TestTours.bookableDeparture(mvc, admin, 5, 20));
        mvc.perform(post("/api/v1/payments/payos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookingCode\":\"%s\",\"phone\":\"0999999999\",\"type\":\"FULL\"}".formatted(b.code())))
                .andExpect(status().isNotFound());
    }

    // ---------- payOS giả ----------

    /**
     * Tạo link: kiểm tra khóa trong tiêu đề và chữ ký theo đúng công thức tài liệu payOS (tính lại ở đây, độc lập với
     * {@link PayOsGateway}); sai thì trả lỗi như payOS thật. Hỏi trạng thái: trả trạng thái test đã đặt cho link.
     */
    private static HttpServer fakePayOs() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/v2/payment-requests", exchange -> {
                if (!"test-client-id".equals(exchange.getRequestHeaders().getFirst("x-client-id"))
                        || !"test-api-key".equals(exchange.getRequestHeaders().getFirst("x-api-key"))) {
                    reply(exchange, 401, Map.of("code", "401", "desc", "Sai khóa"));
                } else if ("POST".equals(exchange.getRequestMethod())) {
                    String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                    long orderCode = ((Number) JsonPath.read(body, "$.orderCode")).longValue();
                    int amount = JsonPath.read(body, "$.amount");
                    String expected = hmac("amount=" + amount + "&cancelUrl=" + JsonPath.read(body, "$.cancelUrl")
                            + "&description=" + JsonPath.read(body, "$.description") + "&orderCode=" + orderCode
                            + "&returnUrl=" + JsonPath.read(body, "$.returnUrl"));
                    if (!expected.equals(JsonPath.read(body, "$.signature"))) {
                        reply(exchange, 200, Map.of("code", "20", "desc", "Chữ ký không hợp lệ"));
                        return;
                    }
                    LINKS.put(orderCode, new HashMap<>(Map.of("amount", amount, "status", "PENDING")));
                    reply(exchange, 200, Map.of("code", "00", "desc", "success", "data",
                            Map.of("checkoutUrl", "https://pay.payos.vn/web/test", "qrCode", "00020101021238570010A000000727", "paymentLinkId", "link" + orderCode)));
                } else {
                    long orderCode = Long.parseLong(exchange.getRequestURI().getPath().substring("/v2/payment-requests/".length()));
                    Map<String, Object> link = LINKS.get(orderCode);
                    int amount = (int) link.get("amount");
                    boolean paid = "PAID".equals(link.get("status"));
                    List<Map<String, Object>> transactions = paid ? List.of(Map.of("reference", "FT" + orderCode, "amount", amount,
                            "transactionDateTime", "2026-11-14 10:30:00", "counterAccountBankId", "970422")) : List.of();
                    reply(exchange, 200, Map.of("code", "00", "desc", "success", "data", Map.of("orderCode", orderCode, "amount", amount,
                            "amountPaid", paid ? amount : 0, "status", link.get("status"), "transactions", transactions)));
                }
            });
            server.start();
            return server;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void reply(HttpExchange exchange, int status, Map<String, ?> body) throws IOException {
        byte[] bytes = new JsonMapper().writeValueAsBytes(body);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private static String hmac(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(CHECKSUM_KEY.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static void payOsMarks(String ref, String status) {
        LINKS.get(Long.parseLong(ref)).put("status", status);
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
        return mvc.perform(post("/api/v1/payments/payos").contentType(MediaType.APPLICATION_JSON)
                .content("{\"bookingCode\":\"%s\",\"phone\":\"%s\",\"type\":\"%s\"}".formatted(code, PHONE, type)));
    }

    private static String reference(ResultActions linkResult) throws Exception {
        return JsonPath.read(linkResult.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8),
                "$.reference");
    }

    /** Phần {@code data} payOS gửi trong webhook khi có tiền vào. */
    private static Map<String, Object> webhookData(String ref, long amount) {
        Map<String, Object> d = new HashMap<>();
        d.put("orderCode", Long.parseLong(ref));
        d.put("amount", amount);
        d.put("description", "VLTEST");
        d.put("accountNumber", "12345678");
        d.put("reference", "FT" + ref);
        d.put("transactionDateTime", "2026-11-14 10:30:00");
        d.put("currency", "VND");
        d.put("paymentLinkId", "link" + ref);
        d.put("code", "00");
        d.put("desc", "success");
        d.put("counterAccountBankId", "970422");
        d.put("counterAccountName", null);
        return d;
    }

    private ResultActions webhook(String ref, long amount) throws Exception {
        Map<String, Object> data = webhookData(ref, amount);
        return postWebhook(Map.of("code", "00", "desc", "success", "success", true, "data", data, "signature", gateway.signData(data)));
    }

    private ResultActions postWebhook(Map<String, ?> body) throws Exception {
        return mvc.perform(post("/api/v1/payments/payos/webhook").contentType(MediaType.APPLICATION_JSON)
                .content(new JsonMapper().writeValueAsString(body)));
    }

    private ResultActions returnPage(String orderCode) throws Exception {
        return mvc.perform(get("/api/v1/payments/payos/return").param("orderCode", orderCode));
    }

    private ResultActions manual(long bookingId, long amount) throws Exception {
        return mvc.perform(post("/api/v1/admin/bookings/{id}/payments", bookingId).header("Authorization", admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\":%d,\"method\":\"BANK_TRANSFER\",\"transactionNo\":\"CK001\",\"proofUrl\":\"/uploads/ck.jpg\"}".formatted(amount)));
    }

    private ResultActions lookup(String code) throws Exception {
        return mvc.perform(get("/api/v1/bookings/lookup").param("code", code).param("phone", PHONE));
    }

    private static LocalDateTime now() {
        return LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
    }
}
