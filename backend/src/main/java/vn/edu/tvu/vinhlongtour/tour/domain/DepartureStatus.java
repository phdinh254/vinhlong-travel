package vn.edu.tvu.vinhlongtour.tour.domain;

/**
 * Trạng thái do admin đặt cho đợt khởi hành. "Hết chỗ" không lưu riêng mà suy ra từ số chỗ còn lại.
 */
public enum DepartureStatus {
    /** Đang nhận khách. */
    OPEN,
    /** Ngừng nhận khách (vẫn giữ các đơn đã đặt). */
    CLOSED,
    /** Hủy đợt; chỉ được khi chưa có khách đặt. */
    CANCELLED
}
