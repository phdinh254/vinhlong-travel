package vn.edu.tvu.vinhlongtour.payment;

import org.junit.jupiter.api.Test;
import vn.edu.tvu.vinhlongtour.common.ApiException;
import vn.edu.tvu.vinhlongtour.common.AppProperties;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Kiểm tra chữ ký VNPay: khớp với một cài đặt độc lập (Python hmac), ký rồi kiểm lại được,
 * và sửa bất kỳ giá trị nào thì chữ ký không còn đúng.
 */
class VnPayGatewayTest {

    private static VnPayGateway gateway(String secret) {
        return new VnPayGateway(new AppProperties(null, null, null, null, null, new AppProperties.VnPay(
                "TESTTMN1", secret, "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html",
                "http://localhost:5173/thanh-toan/ket-qua", Duration.ofMinutes(15), true), null));
    }

    @Test
    void signatureMatchesIndependentPythonImplementation() {
        Map<String, String> params = new HashMap<>();
        params.put("vnp_Amount", "27000000");
        params.put("vnp_Command", "pay");
        params.put("vnp_OrderInfo", "Thanh toan don VL261114ABCDE");
        params.put("vnp_TxnRef", "VL261114ABCDE-7");
        params.put("vnp_ReturnUrl", "http://localhost:5173/thanh-toan/ket-qua");

        // Tính bằng: hmac.new(b'KHOA-MAU-VNPAY', canonical, hashlib.sha512) với canonical sắp xếp + quote_plus
        assertThat(gateway("KHOA-MAU-VNPAY").sign(params)).isEqualTo(
                "c0ae5f8557a56602c2e9266f5c89c3699866cf0e5ad60438ab78ea9d810e8f62871be64649227bb8c2d2c1f367820b88fdeb4ea2f2eb71792da06720a08127c5");
    }

    @Test
    void paymentUrlCarriesAmountTimes100AndAValidSignature() {
        VnPayGateway gateway = gateway("KHOA-MAU-VNPAY");

        String url = gateway.paymentUrl("VL261114ABCDE-7", 270_000, "Thanh toan don VL261114ABCDE", "127.0.0.1",
                LocalDateTime.of(2026, 11, 14, 10, 0));
        Map<String, String> params = query(url);

        assertThat(params).containsEntry("vnp_Amount", "27000000")
                .containsEntry("vnp_CreateDate", "20261114100000")
                .containsEntry("vnp_ExpireDate", "20261114101500")
                .containsEntry("vnp_TmnCode", "TESTTMN1");
        assertThat(gateway.verify(params)).isTrue();
    }

    @Test
    void tamperedOrUnsignedDataIsRejected() {
        VnPayGateway gateway = gateway("KHOA-MAU-VNPAY");
        Map<String, String> params = new HashMap<>(Map.of("vnp_Amount", "27000000", "vnp_TxnRef", "VL1-1", "vnp_ResponseCode", "00"));
        params.put("vnp_SecureHash", gateway.sign(params).toUpperCase());   // VNPay có thể gửi chữ hoa
        assertThat(gateway.verify(params)).isTrue();

        params.put("vnp_Amount", "1000");                                      // đổi số tiền
        assertThat(gateway.verify(params)).isFalse();

        params.remove("vnp_SecureHash");
        assertThat(gateway.verify(params)).isFalse();
        assertThat(gateway("KHOA-KHAC").verify(Map.of("vnp_Amount", "1", "vnp_SecureHash", gateway.sign(Map.of("vnp_Amount", "1"))))).isFalse();
    }

    @Test
    void missingConfigurationMeansVnPayIsUnavailable() {
        assertThatThrownBy(() -> gateway("").paymentUrl("X-1", 1000, "x", "127.0.0.1", LocalDateTime.now()))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getCode()).isEqualTo("VNPAY_NOT_CONFIGURED"));
    }

    private static Map<String, String> query(String url) {
        Map<String, String> params = new LinkedHashMap<>();
        for (String pair : URI.create(url).getRawQuery().split("&")) {
            String[] kv = pair.split("=", 2);
            params.put(URLDecoder.decode(kv[0], StandardCharsets.US_ASCII), URLDecoder.decode(kv[1], StandardCharsets.US_ASCII));
        }
        return params;
    }
}
