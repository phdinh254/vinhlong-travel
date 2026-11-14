package vn.edu.tvu.vinhlongtour.content.service;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Tạo đường dẫn thân thiện (slug) từ tiêu đề tiếng Việt: bỏ dấu, chữ thường, nối bằng gạch ngang.
 * Ví dụ "Lễ hội Ok Om Bok 2026" → "le-hoi-ok-om-bok-2026".
 */
final class Slugs {

    static final int MAX_LENGTH = 200;

    private Slugs() {
    }

    static String of(String text) {
        String noMarks = Normalizer.normalize(text.replace('đ', 'd').replace('Đ', 'D'), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        String slug = noMarks.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        if (slug.length() > MAX_LENGTH) {
            slug = slug.substring(0, MAX_LENGTH).replaceAll("-$", "");
        }
        return slug.isEmpty() ? "bai-viet" : slug;
    }
}
