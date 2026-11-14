package vn.edu.tvu.vinhlongtour.upload;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Kiểm tra nhận diện ảnh bằng các byte đầu file.
 */
class ImageStorageTest {

    private static byte[] bytes(int... values) {
        byte[] b = new byte[values.length];
        for (int i = 0; i < values.length; i++) {
            b[i] = (byte) values[i];
        }
        return b;
    }

    @Test
    void recognisesImageSignatures() {
        assertThat(ImageStorage.imageExtension(bytes(0xFF, 0xD8, 0xFF, 0xE0))).isEqualTo("jpg");
        assertThat(ImageStorage.imageExtension(bytes(0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A))).isEqualTo("png");
        assertThat(ImageStorage.imageExtension("GIF89a".getBytes(StandardCharsets.US_ASCII))).isEqualTo("gif");
        assertThat(ImageStorage.imageExtension("RIFF\0\0\0\0WEBP".getBytes(StandardCharsets.US_ASCII))).isEqualTo("webp");
    }

    @Test
    void rejectsEverythingElse() {
        assertThat(ImageStorage.imageExtension("<html><script>".getBytes(StandardCharsets.US_ASCII))).isNull();
        assertThat(ImageStorage.imageExtension("<svg onload=x>".getBytes(StandardCharsets.US_ASCII))).isNull();
        assertThat(ImageStorage.imageExtension("RIFF\0\0\0\0WAVE".getBytes(StandardCharsets.US_ASCII))).isNull();
        assertThat(ImageStorage.imageExtension(bytes(0xFF, 0xD8))).isNull();
        assertThat(ImageStorage.imageExtension(new byte[0])).isNull();
    }
}
