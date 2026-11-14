package vn.edu.tvu.vinhlongtour.booking.domain;

/**
 * Trạng thái từng hành khách trong đơn; hủy từng người (giai đoạn 6) chỉ đổi người đó sang {@code CANCELLED}.
 */
public enum PassengerStatus {
    ACTIVE,
    CANCELLED
}
