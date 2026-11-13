package vn.edu.tvu.vinhlongtour.common;

import java.util.Map;

/**
 * Dạng JSON chung của mọi phản hồi lỗi:
 * {@code { "code": "SEAT_NOT_ENOUGH", "message": "...", "fields": { "email": "..." } }}.
 * {@code fields} chỉ có khi dữ liệu gửi lên sai ở từng trường; các lỗi khác để rỗng.
 */
public record ApiError(String code, String message, Map<String, String> fields) {

    public ApiError(String code, String message) {
        this(code, message, Map.of());
    }
}
