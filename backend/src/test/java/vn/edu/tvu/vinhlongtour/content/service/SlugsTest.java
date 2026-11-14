package vn.edu.tvu.vinhlongtour.content.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Kiểm tra tạo slug từ tiêu đề tiếng Việt.
 */
class SlugsTest {

    @Test
    void removesVietnameseMarksAndJoinsWithDashes() {
        assertThat(Slugs.of("Lễ hội Ok Om Bok 2026")).isEqualTo("le-hoi-ok-om-bok-2026");
        assertThat(Slugs.of("Đường về Cù lao Dài — Vũng Liêm!")).isEqualTo("duong-ve-cu-lao-dai-vung-liem");
        assertThat(Slugs.of("  Ăn gì ở Bến Tre?  ")).isEqualTo("an-gi-o-ben-tre");
    }

    @Test
    void neverEmptyAndNeverTooLong() {
        assertThat(Slugs.of("!!!")).isEqualTo("bai-viet");
        assertThat(Slugs.of("a ".repeat(300))).hasSizeLessThanOrEqualTo(Slugs.MAX_LENGTH).doesNotEndWith("-");
    }
}
