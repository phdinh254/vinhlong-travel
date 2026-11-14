package vn.edu.tvu.vinhlongtour.booking.domain;

import java.util.EnumSet;
import java.util.Set;

/**
 * Trạng thái đơn đặt tour và các bước chuyển hợp lệ. Mỗi trạng thái tự biết được phép đi tới đâu,
 * nên mọi nơi đổi trạng thái đều qua {@link #canMoveTo(BookingStatus)} thay vì so chuỗi rải rác.
 * <pre>
 * PENDING_PAYMENT ─┬─> DEPOSITED ─┬─> PAID ──> COMPLETED
 *                  ├──────────────┼─> PAID
 *                  ├─> EXPIRED ───┴─> (PENDING_PAYMENT khi nhận tiền trễ, giai đoạn 5)
 *                  └─> CANCELLED <── DEPOSITED, PAID
 * </pre>
 */
public enum BookingStatus {

    /** Vừa đặt, đang giữ chỗ chờ thanh toán. */
    PENDING_PAYMENT,
    /** Đã đặt cọc, chưa trả đủ. */
    DEPOSITED,
    /** Đã trả đủ. */
    PAID,
    /** Chuyến đi đã kết thúc. */
    COMPLETED,
    /** Đã hủy, chỗ đã trả lại. */
    CANCELLED,
    /** Quá hạn giữ chỗ mà chưa trả đồng nào, chỗ đã trả lại. */
    EXPIRED;

    public Set<BookingStatus> nextStatuses() {
        return switch (this) {
            case PENDING_PAYMENT -> EnumSet.of(DEPOSITED, PAID, CANCELLED, EXPIRED);
            case DEPOSITED -> EnumSet.of(PAID, CANCELLED);
            case PAID -> EnumSet.of(COMPLETED, CANCELLED);
            case EXPIRED -> EnumSet.of(PENDING_PAYMENT);
            case COMPLETED, CANCELLED -> EnumSet.noneOf(BookingStatus.class);
        };
    }

    public boolean canMoveTo(BookingStatus target) {
        return nextStatuses().contains(target);
    }

    /** Đơn còn hiệu lực: đang giữ chỗ trên đợt khởi hành. */
    public boolean holdsSeats() {
        return this == PENDING_PAYMENT || this == DEPOSITED || this == PAID;
    }
}
