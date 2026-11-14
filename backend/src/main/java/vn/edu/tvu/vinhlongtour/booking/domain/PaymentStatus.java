package vn.edu.tvu.vinhlongtour.booking.domain;

/**
 * Trạng thái một lần thanh toán. Chỉ {@code PENDING} mới đổi được; đã thành công/thất bại thì giữ nguyên
 * (VNPay gửi lại thông báo cũng không ghi nhận thêm lần nữa).
 */
public enum PaymentStatus {
    PENDING,
    SUCCEEDED,
    FAILED
}
