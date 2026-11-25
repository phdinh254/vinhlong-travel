package vn.edu.tvu.vinhlongtour.payment;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;
import vn.edu.tvu.vinhlongtour.common.ApiException;
import vn.edu.tvu.vinhlongtour.common.AppProperties;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Làm việc với cổng payOS (API v2): tạo link thanh toán, hỏi trạng thái một giao dịch, kiểm tra chữ ký webhook.
 * <ul>
 *   <li>Mọi lời gọi API gửi kèm tiêu đề {@code x-client-id}, {@code x-api-key}.</li>
 *   <li>Chữ ký là HMAC-SHA256 (dạng hex) bằng {@code checksumKey}, ký trên chuỗi {@code key=value} nối bằng {@code &}
 *       với các khóa sắp xếp theo bảng chữ cái; giá trị rỗng/null ghi là chuỗi rỗng.</li>
 *   <li>Trang payOS chuyển khách về ({@code returnUrl}) không có chữ ký, nên kết quả luôn được xác minh lại bằng
 *       {@link #paymentInfo(long)} hoặc webhook có chữ ký.</li>
 * </ul>
 */
@Component
public class PayOsGateway {

    /** Link vừa tạo: trang thanh toán của payOS và chuỗi VietQR để hiển thị mã QR. */
    public record CreatedLink(String checkoutUrl, String qrCode, String paymentLinkId) {}

    /** Một giao dịch chuyển khoản payOS ghi nhận cho link. */
    public record Transaction(String reference, long amount, String transactionDateTime, String counterAccountBankId) {}

    /** Trạng thái link trên payOS: PENDING, PROCESSING, PAID, CANCELLED, EXPIRED... */
    public record PaymentInfo(long orderCode, String status, long amount, long amountPaid, List<Transaction> transactions) {
        public boolean isPaid() {
            return "PAID".equals(status);
        }

        public boolean isClosedWithoutPayment() {
            return "CANCELLED".equals(status) || "EXPIRED".equals(status);
        }
    }

    private final AppProperties.PayOs config;
    private final JsonMapper json;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    public PayOsGateway(AppProperties props, JsonMapper json) {
        this.config = props.payos();
        this.json = json;
    }

    /**
     * Tạo link thanh toán. {@code description} tối đa 9 ký tự (giới hạn của payOS với tài khoản ngân hàng
     * không liên kết trực tiếp); {@code expiredAt} là Unix timestamp (giây).
     */
    public CreatedLink createPaymentLink(long orderCode, long amount, String description, long expiredAt) {
        ensureConfigured();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("orderCode", orderCode);
        body.put("amount", amount);
        body.put("description", description);
        body.put("cancelUrl", config.cancelUrl());
        body.put("returnUrl", config.returnUrl());
        body.put("expiredAt", expiredAt);
        body.put("signature", signPaymentRequest(orderCode, amount, description));
        Map<String, Object> data = call("POST", "/v2/payment-requests", body);
        return new CreatedLink(str(data.get("checkoutUrl")), str(data.get("qrCode")), str(data.get("paymentLinkId")));
    }

    /** Hỏi payOS trạng thái thật của một link (theo mã đơn hàng đã gửi khi tạo link). */
    public PaymentInfo paymentInfo(long orderCode) {
        ensureConfigured();
        Map<String, Object> data = call("GET", "/v2/payment-requests/" + orderCode, null);
        List<Transaction> transactions = data.get("transactions") instanceof Collection<?> list
                ? list.stream().filter(Map.class::isInstance).map(t -> (Map<?, ?>) t)
                        .map(t -> new Transaction(str(t.get("reference")), num(t.get("amount")), str(t.get("transactionDateTime")),
                                str(t.get("counterAccountBankId"))))
                        .toList()
                : List.of();
        return new PaymentInfo(num(data.get("orderCode")), str(data.get("status")), num(data.get("amount")),
                num(data.get("amountPaid")), transactions);
    }

    /** Chữ ký của yêu cầu tạo link: chỉ ký 5 trường amount, cancelUrl, description, orderCode, returnUrl. */
    public String signPaymentRequest(long orderCode, long amount, String description) {
        return hmacSha256("amount=" + amount + "&cancelUrl=" + config.cancelUrl() + "&description=" + description
                + "&orderCode=" + orderCode + "&returnUrl=" + config.returnUrl());
    }

    /** Chữ ký của phần {@code data} trong webhook. Dùng cả khi giả lập payOS trong test. */
    public String signData(Map<String, ?> data) {
        String canonical = new TreeMap<>(data).entrySet().stream()
                .map(e -> e.getKey() + "=" + canonicalValue(e.getValue()))
                .collect(Collectors.joining("&"));
        return hmacSha256(canonical);
    }

    /** Webhook có đúng chữ ký không (so sánh thời gian cố định, không phân biệt hoa thường). */
    public boolean verifyData(Map<String, ?> data, String signature) {
        if (data == null || signature == null || isBlank(config.checksumKey())) {
            return false;
        }
        return MessageDigest.isEqual(signData(data).getBytes(StandardCharsets.US_ASCII),
                signature.toLowerCase().getBytes(StandardCharsets.US_ASCII));
    }

    // null/"null"/"undefined" ghi là chuỗi rỗng; mảng, đối tượng ghi dạng JSON như mẫu của payOS
    private String canonicalValue(Object value) {
        if (value == null || "null".equals(value) || "undefined".equals(value)) {
            return "";
        }
        if (value instanceof Map<?, ?> || value instanceof Collection<?>) {
            return json.writeValueAsString(value);
        }
        return String.valueOf(value);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> call(String method, String path, Map<String, Object> body) {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(config.apiUrl() + path))
                .timeout(Duration.ofSeconds(20))
                .header("x-client-id", config.clientId())
                .header("x-api-key", config.apiKey())
                .header("Content-Type", "application/json");
        request = body == null ? request.GET() : request.method(method, HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)));
        HttpResponse<String> response;
        try {
            response = http.send(request.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "PAYOS_UNAVAILABLE", "Không kết nối được cổng thanh toán payOS, vui lòng thử lại");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException(HttpStatus.BAD_GATEWAY, "PAYOS_UNAVAILABLE", "Không kết nối được cổng thanh toán payOS, vui lòng thử lại");
        }
        Map<String, Object> result;
        try {
            result = json.readValue(response.body(), Map.class);
        } catch (RuntimeException e) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "PAYOS_ERROR", "payOS trả về dữ liệu không đọc được (HTTP " + response.statusCode() + ")");
        }
        if (response.statusCode() / 100 != 2 || !"00".equals(str(result.get("code"))) || !(result.get("data") instanceof Map<?, ?>)) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "PAYOS_ERROR",
                    "payOS từ chối yêu cầu: " + str(result.get("desc")) + " (mã " + str(result.get("code")) + ")");
        }
        return (Map<String, Object>) result.get("data");
    }

    private void ensureConfigured() {
        if (isBlank(config.clientId()) || isBlank(config.apiKey()) || isBlank(config.checksumKey())) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "PAYOS_NOT_CONFIGURED",
                    "Máy chủ chưa cấu hình payOS (PAYOS_CLIENT_ID, PAYOS_API_KEY, PAYOS_CHECKSUM_KEY)");
        }
    }

    private String hmacSha256(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(config.checksumKey().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return java.util.HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Không tạo được chữ ký payOS", e);
        }
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private static long num(Object o) {
        return o instanceof Number n ? n.longValue() : o == null ? 0 : Long.parseLong(String.valueOf(o));
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
