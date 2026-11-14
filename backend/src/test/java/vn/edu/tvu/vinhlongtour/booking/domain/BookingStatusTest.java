package vn.edu.tvu.vinhlongtour.booking.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Kiểm tra bảng chuyển trạng thái đơn: chỉ các bước hợp lệ mới được phép.
 */
class BookingStatusTest {

    @Test
    void pendingBookingCanBePaidCancelledOrExpire() {
        assertThat(BookingStatus.PENDING_PAYMENT.nextStatuses())
                .containsExactlyInAnyOrder(BookingStatus.DEPOSITED, BookingStatus.PAID, BookingStatus.CANCELLED, BookingStatus.EXPIRED);
    }

    @Test
    void onlyPaidBookingCanBeCompleted() {
        assertThat(BookingStatus.PAID.canMoveTo(BookingStatus.COMPLETED)).isTrue();
        assertThat(BookingStatus.PENDING_PAYMENT.canMoveTo(BookingStatus.COMPLETED)).isFalse();
        assertThat(BookingStatus.DEPOSITED.canMoveTo(BookingStatus.COMPLETED)).isFalse();
    }

    @Test
    void finishedBookingsCannotChange() {
        assertThat(BookingStatus.COMPLETED.nextStatuses()).isEmpty();
        assertThat(BookingStatus.CANCELLED.nextStatuses()).isEmpty();
        assertThat(BookingStatus.EXPIRED.canMoveTo(BookingStatus.CANCELLED)).isFalse();
    }

    @Test
    void onlyActiveBookingsHoldSeats() {
        assertThat(BookingStatus.PENDING_PAYMENT.holdsSeats()).isTrue();
        assertThat(BookingStatus.PAID.holdsSeats()).isTrue();
        assertThat(BookingStatus.EXPIRED.holdsSeats()).isFalse();
        assertThat(BookingStatus.CANCELLED.holdsSeats()).isFalse();
    }
}
