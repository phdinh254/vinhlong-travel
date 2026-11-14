package vn.edu.tvu.vinhlongtour.tour.domain;

/**
 * Trạng thái của tour. Khách chỉ thấy tour {@code ON_SALE}.
 */
public enum TourStatus {
    /** Đang soạn, chưa hiển thị. */
    DRAFT,
    /** Đang bán: phải có ảnh đại diện và lịch trình đủ số ngày. */
    ON_SALE,
    /** Ngừng bán, ẩn khỏi trang khách nhưng vẫn giữ dữ liệu. */
    STOPPED
}
