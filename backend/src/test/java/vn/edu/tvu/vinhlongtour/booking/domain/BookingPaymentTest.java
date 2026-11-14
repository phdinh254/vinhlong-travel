package vn.edu.tvu.vinhlongtour.booking.domain;

import org.junit.jupiter.api.Test;
import vn.edu.tvu.vinhlongtour.catalog.domain.AgeRange;
import vn.edu.tvu.vinhlongtour.catalog.domain.PassengerType;
import vn.edu.tvu.vinhlongtour.common.ApiException;
import vn.edu.tvu.vinhlongtour.tour.domain.Departure;
import vn.edu.tvu.vinhlongtour.tour.domain.Itinerary;
import vn.edu.tvu.vinhlongtour.tour.domain.Tour;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Kiểm tra cách đơn nhận tiền: cọc rồi trả đủ, trả thừa, nhận tiền khi đơn đã hủy,
 * và số tiền cần hoàn tự tính ra đúng.
 */
class BookingPaymentTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 14, 9, 0);
    private static final long TOTAL = 1_000_000;

    private static Booking booking() {
        Tour tour = new Tour("VL1N", "Vĩnh Long 1 ngày", 1, 0);
        tour.replaceItinerary(List.of(new Itinerary.DayPlan("Ngày 1", null, List.of())));
        Departure departure = new Departure(tour, NOW.plusDays(10), NOW.plusDays(10).plusHours(9), NOW.plusDays(9), 20, NOW);
        PassengerType adult = new PassengerType("Người lớn", new AgeRange(12, 120), 1);
        Passenger.Details details = new Passenger.Details("A", LocalDate.of(1990, 1, 1), null, null, null, adult, TOTAL, null, null, 0);
        return new Booking("VL261114AAAAA", departure, null, new Booking.Contact("A", "0901234567", null, null),
                List.of(details), List.of(), null, NOW, NOW.plusMinutes(30));
    }

    @Test
    void depositThenBalanceMovesBookingToPaid() {
        Booking b = booking();

        b.confirmOnlinePayment(b.startOnlinePayment(300_000, NOW), "VNP1", "NCB", NOW, null, NOW);
        assertThat(b.getStatus()).isEqualTo(BookingStatus.DEPOSITED);
        assertThat(b.getHoldUntil()).isNull();
        assertThat(b.remainingAmount()).isEqualTo(700_000);

        b.recordManualPayment(700_000, PaymentMethod.BANK_TRANSFER, "CK123", "/uploads/ck.jpg", null, null, NOW);
        assertThat(b.getStatus()).isEqualTo(BookingStatus.PAID);
        assertThat(b.refundDue()).isZero();
    }

    @Test
    void secondPaymentAfterPaidInFullIsFlaggedForRefund() {
        Booking b = booking();
        Payment first = b.startOnlinePayment(TOTAL, NOW);
        b.recordManualPayment(TOTAL, PaymentMethod.CASH, null, null, null, null, NOW);   // khách trả tiền mặt trước

        b.confirmOnlinePayment(first, "VNP2", "NCB", NOW, null, NOW);                    // rồi link VNPay cũ cũng được trả

        assertThat(b.getStatus()).isEqualTo(BookingStatus.PAID);
        assertThat(b.getPaidAmount()).isEqualTo(2 * TOTAL);
        assertThat(b.refundDue()).isEqualTo(TOTAL);
        assertThat(b.getHistory().getLast().getReason()).contains("CẦN HOÀN");
    }

    @Test
    void moneyArrivingAfterCancellationMustBeRefundedInFull() {
        Booking b = booking();
        Payment payment = b.startOnlinePayment(300_000, NOW);
        b.moveTo(BookingStatus.CANCELLED, null, "Admin hủy", NOW);

        b.confirmOnlinePayment(payment, "VNP3", "NCB", NOW, null, NOW);

        assertThat(b.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(b.refundDue()).isEqualTo(300_000);
    }

    @Test
    void adminCannotRecordMoreThanRemaining() {
        assertThatThrownBy(() -> booking().recordManualPayment(TOTAL + 1, PaymentMethod.CASH, null, null, null, null, NOW))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getCode()).isEqualTo("AMOUNT_EXCEEDS_REMAINING"));
    }

    @Test
    void bookingIsPayableOnlyWhileHoldIsValidOrDeposited() {
        Booking b = booking();
        assertThat(b.isPayable(NOW.plusMinutes(10))).isTrue();
        assertThat(b.isPayable(NOW.plusMinutes(31))).isFalse();

        b.recordManualPayment(100_000, PaymentMethod.CASH, null, null, null, null, NOW);
        assertThat(b.isPayable(NOW.plusDays(5))).as("đã cọc thì không còn hạn giữ chỗ").isTrue();
    }
}
