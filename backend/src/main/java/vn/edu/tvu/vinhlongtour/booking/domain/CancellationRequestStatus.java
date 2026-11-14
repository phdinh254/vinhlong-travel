package vn.edu.tvu.vinhlongtour.booking.domain;

/**
 * Trạng thái yêu cầu hủy. Yêu cầu đang chờ tự chuyển sang {@code REJECTED} (kèm ghi chú) nếu đơn
 * bị hủy hoặc hết hạn bằng cách khác trước khi được duyệt.
 */
public enum CancellationRequestStatus {
    PENDING,
    APPROVED,
    REJECTED
}
