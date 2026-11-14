package vn.edu.tvu.vinhlongtour.catalog.domain;

/**
 * Độ tin cậy của giờ mở cửa hoặc giá vé điểm đến (xem docs/data/README.md).
 */
public enum InfoQuality {
    /** Có trong nguồn tham khảo. */
    SOURCE,
    /** Nguồn ghi "mở cả ngày", hệ thống quy về 06:00–18:00. */
    ALL_DAY,
    /** Không có nguồn, ước tính. */
    ESTIMATED,
    /** Chỉ thấy trong tóm tắt kết quả tìm kiếm, chưa mở được trang gốc. */
    UNVERIFIED,
    /** Không tìm được thông tin. */
    UNKNOWN
}
