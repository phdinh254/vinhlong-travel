package vn.edu.tvu.vinhlongtour.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vn.edu.tvu.vinhlongtour.config.AppProperties;
import vn.edu.tvu.vinhlongtour.exception.ApiException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Cổng VNPay phiên bản 2.1.0:
 *  - Tham số sắp xếp theo tên (a-z), giá trị URL-encode, nối bằng '&'
 *  - vnp_SecureHash = HMAC-SHA512(hashSecret, chuỗi trên)
 *  - vnp_Amount = số tiền VND x 100
 */
@Service
@RequiredArgsConstructor
public class VnPayService {
    public static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");

    private final AppProperties props;

    public String createPaymentUrl(String txnRef, BigDecimal soTien, String orderInfo, String ipAddr) {
        AppProperties.VnPay cfg = props.vnpay();
        if (isBlank(cfg.tmnCode()) || isBlank(cfg.hashSecret())) {
            throw ApiException.badRequest("Máy chủ chưa cấu hình VNPAY_TMN_CODE / VNPAY_HASH_SECRET");
        }
        LocalDateTime now = LocalDateTime.now(VN);
        Map<String, String> p = new TreeMap<>();
        p.put("vnp_Version", "2.1.0");
        p.put("vnp_Command", "pay");
        p.put("vnp_TmnCode", cfg.tmnCode());
        p.put("vnp_Amount", soTien.multiply(BigDecimal.valueOf(100)).toBigInteger().toString());
        p.put("vnp_CurrCode", "VND");
        p.put("vnp_TxnRef", txnRef);
        p.put("vnp_OrderInfo", orderInfo);
        p.put("vnp_OrderType", "other");
        p.put("vnp_Locale", "vn");
        p.put("vnp_ReturnUrl", cfg.returnUrl());
        p.put("vnp_IpAddr", ipAddr == null ? "127.0.0.1" : ipAddr);
        p.put("vnp_CreateDate", now.format(FMT));
        p.put("vnp_ExpireDate", now.plusMinutes(15).format(FMT));

        String query = buildQuery(p);
        return cfg.payUrl() + "?" + query + "&vnp_SecureHash=" + hmacSha512(cfg.hashSecret(), query);
    }

    /** Kiểm tra chữ ký dữ liệu VNPay gửi về (return URL hoặc IPN) */
    public boolean verify(Map<String, String> params) {
        String received = params.get("vnp_SecureHash");
        if (received == null || isBlank(props.vnpay().hashSecret())) return false;
        Map<String, String> data = params.entrySet().stream()
                .filter(e -> e.getKey().startsWith("vnp_"))
                .filter(e -> !e.getKey().equals("vnp_SecureHash") && !e.getKey().equals("vnp_SecureHashType"))
                .filter(e -> e.getValue() != null && !e.getValue().isEmpty())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, TreeMap::new));
        String expected = hmacSha512(props.vnpay().hashSecret(), buildQuery(data));
        return MessageDigest.isEqual(expected.toLowerCase().getBytes(StandardCharsets.US_ASCII),
                received.toLowerCase().getBytes(StandardCharsets.US_ASCII));
    }

    static String buildQuery(Map<String, String> sorted) {
        return sorted.entrySet().stream()
                .filter(e -> e.getValue() != null && !e.getValue().isEmpty())
                .map(e -> enc(e.getKey()) + "=" + enc(e.getValue()))
                .collect(Collectors.joining("&"));
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.US_ASCII);
    }

    static String hmacSha512(String key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Không tạo được chữ ký VNPay", e);
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
