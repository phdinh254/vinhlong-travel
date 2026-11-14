package vn.edu.tvu.vinhlongtour.common;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Dạng JSON chung cho mọi danh sách có phân trang: {@code { items, page, size, total }}.
 * {@code page} đếm từ 0.
 */
public record PageResponse<T>(List<T> items, int page, int size, long total) {

    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements());
    }
}
