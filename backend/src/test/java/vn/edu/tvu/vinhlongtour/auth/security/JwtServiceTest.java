package vn.edu.tvu.vinhlongtour.auth.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Kiểm tra cách chọn khóa ký token: khóa quá ngắn bị từ chối ngay lúc khởi động,
 * không đặt khóa thì sinh khóa ngẫu nhiên đủ 256 bit (mỗi lần một khóa khác).
 */
class JwtServiceTest {

    @Test
    void secretShorterThan32CharactersIsRejected() {
        assertThatThrownBy(() -> JwtService.secretKey("ngan-qua"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void blankSecretGeneratesDifferentRandom256BitKeys() {
        var first = JwtService.secretKey("");
        var second = JwtService.secretKey(null);

        assertThat(first.getEncoded()).hasSize(32);
        assertThat(first.getEncoded()).isNotEqualTo(second.getEncoded());
    }
}
