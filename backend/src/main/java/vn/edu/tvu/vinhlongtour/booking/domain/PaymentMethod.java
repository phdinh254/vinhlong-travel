package vn.edu.tvu.vinhlongtour.booking.domain;

/**
 * Cách khách trả tiền: qua cổng payOS, hoặc chuyển khoản / tiền mặt do admin xác nhận.
 */
public enum PaymentMethod {
    PAYOS,
    BANK_TRANSFER,
    CASH
}
