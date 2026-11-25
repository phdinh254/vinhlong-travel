package vn.edu.tvu.vinhlongtour.payment;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import vn.edu.tvu.vinhlongtour.common.AppProperties;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Chữ ký webhook payOS: ký trên chuỗi key=value sắp theo khóa, null ghi rỗng, mảng ghi dạng JSON (theo tài liệu payOS). */
class PayOsGatewayTest {

    private static final String KEY = "khoa-checksum-test";

    private final PayOsGateway gateway = new PayOsGateway(new AppProperties(null, null, null, null, null,
            new AppProperties.PayOs("id", "api", KEY, "http://localhost", "http://r", "http://c", Duration.ofMinutes(15)), null),
            new JsonMapper());

    @Test
    void signsSortedCanonicalStringAndVerifiesSignature() throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("orderCode", 123L);
        data.put("amount", 270000);
        data.put("counterAccountName", null);
        data.put("desc", "undefined");
        data.put("items", List.of(Map.of("name", "Tour")));

        String expected = hmac("amount=270000&counterAccountName=&desc=&items=[{\"name\":\"Tour\"}]&orderCode=123");
        assertThat(gateway.signData(data)).isEqualTo(expected);
        assertThat(gateway.verifyData(data, expected.toUpperCase())).isTrue();

        data.put("amount", 1);                                                   // sửa số tiền sau khi ký
        assertThat(gateway.verifyData(data, expected)).isFalse();
        assertThat(gateway.verifyData(null, expected)).isFalse();
    }

    private static String hmac(String s) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(KEY.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(s.getBytes(StandardCharsets.UTF_8)));
    }
}
