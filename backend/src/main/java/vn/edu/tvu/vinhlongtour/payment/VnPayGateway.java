package vn.edu.tvu.vinhlongtour.payment;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import vn.edu.tvu.vinhlongtour.common.ApiException;
import vn.edu.tvu.vinhlongtour.common.AppProperties;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Làm việc với cổng VNPay (API 2.1.0): tạo đường dẫn thanh toán có chữ ký, và kiểm tra chữ ký
 * dữ liệu VNPay gửi về (trang kết quả và IPN).
 * Cách ký theo tài liệu VNPay: sắp xếp tham số {@code vnp_*} theo tên, mã hóa URL tên và giá trị,
 * nối bằng {@code &}, rồi ký HMAC-SHA512 bằng {@code hashSecret}. Số tiền gửi sang = số đồng × 100.
 */
@Component
public class VnPayGateway {

    public static final DateTimeFormatter VNP_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final AppProperties.VnPay config;

    public VnPayGateway(AppProperties props) {
        this.config = props.vnpay();
    }

    /** Đường dẫn chuyển khách sang trang thanh toán VNPay; hết hạn sau {@code linkTimeout}. */
    public String paymentUrl(String txnRef, long amount, String orderInfo, String ipAddress, LocalDateTime now) {
        if (isBlank(config.tmnCode()) || isBlank(config.hashSecret())) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "VNPAY_NOT_CONFIGURED",
                    "Máy chủ chưa cấu hình VNPay (VNPAY_TMN_CODE, VNPAY_HASH_SECRET)");
        }
        Map<String, String> params = new TreeMap<>();
        params.put("vnp_Version", "2.1.0");
        params.put("vnp_Command", "pay");
        params.put("vnp_TmnCode", config.tmnCode());
        params.put("vnp_Amount", String.valueOf(amount * 100));
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_TxnRef", txnRef);
        params.put("vnp_OrderInfo", orderInfo);
        params.put("vnp_OrderType", "other");
        params.put("vnp_Locale", "vn");
        params.put("vnp_ReturnUrl", config.returnUrl());
        params.put("vnp_IpAddr", ipAddress);
        params.put("vnp_CreateDate", now.format(VNP_TIME));
        params.put("vnp_ExpireDate", now.plus(config.linkTimeout()).format(VNP_TIME));
        String query = canonical(params);
        return config.payUrl() + "?" + query + "&vnp_SecureHash=" + hmacSha512(query);
    }

    /** Dữ liệu VNPay gửi về có đúng chữ ký không (so sánh thời gian cố định, không phân biệt hoa thường). */
    public boolean verify(Map<String, String> params) {
        String received = params.get("vnp_SecureHash");
        if (received == null || isBlank(config.hashSecret())) {
            return false;
        }
        String expected = sign(params);
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.US_ASCII),
                received.toLowerCase().getBytes(StandardCharsets.US_ASCII));
    }

    /** Chữ ký của một bộ tham số (bỏ qua chính các trường chữ ký và trường rỗng). Dùng cả khi giả lập VNPay trong test. */
    public String sign(Map<String, String> params) {
        Map<String, String> fields = params.entrySet().stream()
                .filter(e -> e.getKey().startsWith("vnp_"))
                .filter(e -> !e.getKey().equals("vnp_SecureHash") && !e.getKey().equals("vnp_SecureHashType"))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, TreeMap::new));
        return hmacSha512(canonical(fields));
    }

    private static String canonical(Map<String, String> sorted) {
        return sorted.entrySet().stream()
                .filter(e -> e.getValue() != null && !e.getValue().isEmpty())
                .map(e -> encode(e.getKey()) + "=" + encode(e.getValue()))
                .collect(Collectors.joining("&"));
    }

    // VNPay mã hóa theo US-ASCII như URLEncoder (khoảng trắng thành dấu +), giữ nguyên để chữ ký khớp
    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.US_ASCII);
    }

    private String hmacSha512(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(config.hashSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Không tạo được chữ ký VNPay", e);
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
