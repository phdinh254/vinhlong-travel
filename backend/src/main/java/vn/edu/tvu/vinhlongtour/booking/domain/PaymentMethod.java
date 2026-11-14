package vn.edu.tvu.vinhlongtour.booking.domain;

/**
 * Cách khách trả tiền: qua cổng VNPay, hoặc chuyển khoản / tiền mặt do admin xác nhận.
 */
public enum PaymentMethod {
    VNPAY,
    BANK_TRANSFER,
    CASH
}
