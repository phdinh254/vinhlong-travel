package vn.edu.tvu.vinhlongtour.booking.domain;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import vn.edu.tvu.vinhlongtour.catalog.domain.AgeRange;
import vn.edu.tvu.vinhlongtour.catalog.domain.CancellationPolicy;
import vn.edu.tvu.vinhlongtour.catalog.domain.PassengerType;
import vn.edu.tvu.vinhlongtour.catalog.domain.PenaltyTier;
import vn.edu.tvu.vinhlongtour.common.ApiException;
import vn.edu.tvu.vinhlongtour.tour.domain.Departure;
import vn.edu.tvu.vinhlongtour.tour.domain.Itinerary;
import vn.edu.tvu.vinhlongtour.tour.domain.Tour;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Kiểm tra hủy tour, hoàn tiền và chuyển đợt ở lớp domain: phí phạt theo mốc ngày, hủy một phần,
 * và lỗi của bản cũ (duyệt yêu cầu hủy của một đơn đã bị hủy làm trả chỗ hai lần).
 */
class BookingCancellationTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 14, 9, 0);
    private static final long PRICE = 1_000_000;

    private final PassengerType adult = withId(new PassengerType("Người lớn", new AgeRange(12, 120), 1), 1L);
    private final PassengerType child = withId(new PassengerType("Trẻ em", new AgeRange(0, 11), 1), 2L);
    private final CancellationPolicy policy = new CancellationPolicy("Chính sách", NOW.toLocalDate().minusDays(30), null,
            List.of(new PenaltyTier(0, 2, 100), new PenaltyTier(3, 6, 50), new PenaltyTier(7, 14, 20)));

    /** Đơn 2 hành khách đã trả đủ, khởi hành sau {@code daysAhead} ngày. */
    private Booking paidBooking(int daysAhead, Passenger.Details... passengers) {
        Tour tour = new Tour("VL1N", "Vĩnh Long 1 ngày", 1, 0);
        tour.replaceItinerary(List.of(new Itinerary.DayPlan("Ngày 1", null, List.of())));
        Departure d = new Departure(tour, NOW.plusDays(daysAhead), NOW.plusDays(daysAhead).plusHours(9), NOW.plusDays(daysAhead - 1), 20, NOW);
        Booking b = new Booking("VL261114AAAAA", d, null, new Booking.Contact("A", "0901234567", null, null),
                List.of(passengers), List.of(), policy, NOW, NOW.plusMinutes(30));
        long id = 100;
        for (Passenger p : b.getPassengers()) {
            ReflectionTestUtils.setField(p, "id", id++);
        }
        b.recordManualPayment(b.getTotalAmount(), PaymentMethod.CASH, null, null, null, null, NOW);
        return b;
    }

    private Passenger.Details adultPassenger(String name) {
        return new Passenger.Details(name, LocalDate.of(1990, 1, 1), null, null, null, adult, PRICE, null, null, 0);
    }

    private Passenger.Details childPassenger(String name) {
        return new Passenger.Details(name, LocalDate.of(2020, 1, 1), null, null, null, child, PRICE / 2, null, null, 0);
    }

    @Test
    void fullCancellationPenaltyFollowsDaysBeforeDeparture() {
        Booking b = paidBooking(5, adultPassenger("A"), adultPassenger("B"));       // còn 5 ngày: phạt 50%

        CancellationRequest r = b.requestCancellation(CancellationScope.FULL, null, "Bận việc", null, NOW);
        assertThat(r.getPenaltyPercent()).isEqualTo(50);
        assertThat(r.getPenaltyAmount()).isEqualTo(1_000_000);
        assertThat(r.getEstimatedRefund()).isEqualTo(1_000_000);

        int released = b.approveCancellation(r, null, null, NOW);

        assertThat(released).isEqualTo(2);
        assertThat(b.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(b.refundDue()).isEqualTo(1_000_000);
        assertThat(b.activePassengers()).isEmpty();
    }

    @Test
    void partialCancellationKeepsPenaltyInsideTotal() {
        Booking b = paidBooking(10, adultPassenger("A"), adultPassenger("B"));      // còn 10 ngày: phạt 20%
        Long secondId = b.getPassengers().get(1).getId();

        CancellationRequest r = b.requestCancellation(CancellationScope.PARTIAL, Set.of(secondId), null, null, NOW);
        int released = b.approveCancellation(r, null, null, NOW);

        assertThat(released).isEqualTo(1);
        assertThat(b.getSeats()).isEqualTo(1);
        assertThat(b.getTotalAmount()).isEqualTo(PRICE + 200_000);                 // vé người còn lại + phí phạt
        assertThat(b.getStatus()).isEqualTo(BookingStatus.PAID);
        assertThat(b.refundDue()).isEqualTo(800_000);
        assertThat(r.getEstimatedRefund()).isEqualTo(800_000);
    }

    @Test
    void partialCancellationMustLeaveAnAdult() {
        Booking b = paidBooking(10, adultPassenger("Bố"), childPassenger("Con"));
        Long fatherId = b.getPassengers().getFirst().getId();

        assertCode(() -> b.requestCancellation(CancellationScope.PARTIAL, Set.of(fatherId), null, null, NOW), "ADULT_REQUIRED");
    }

    @Test
    void onlyOnePendingRequestAtATime() {
        Booking b = paidBooking(10, adultPassenger("A"), adultPassenger("B"));
        b.requestCancellation(CancellationScope.FULL, null, null, null, NOW);

        assertCode(() -> b.requestCancellation(CancellationScope.FULL, null, null, null, NOW), "CANCELLATION_PENDING");
    }

    @Test
    void pendingRequestOfCancelledBookingCannotBeApprovedAgain() {
        Booking b = paidBooking(10, adultPassenger("A"), adultPassenger("B"));
        CancellationRequest r = b.requestCancellation(CancellationScope.FULL, null, null, null, NOW);

        b.moveTo(BookingStatus.CANCELLED, null, "Hủy bằng cách khác", NOW);       // yêu cầu tự đóng

        assertThat(r.getStatus()).isEqualTo(CancellationRequestStatus.REJECTED);
        assertCode(() -> b.approveCancellation(r, null, null, NOW), "REQUEST_ALREADY_DECIDED");
    }

    @Test
    void refundCannotExceedWhatIsDue() {
        Booking b = paidBooking(5, adultPassenger("A"), adultPassenger("B"));
        b.approveCancellation(b.requestCancellation(CancellationScope.FULL, null, null, null, NOW), null, null, NOW);

        assertCode(() -> b.refund(1_000_001, PaymentMethod.BANK_TRANSFER, null, null, null, null, null, NOW), "REFUND_EXCEEDS_DUE");
        b.refund(600_000, PaymentMethod.BANK_TRANSFER, "HT1", null, null, null, null, NOW);
        b.refund(400_000, PaymentMethod.BANK_TRANSFER, "HT2", null, null, null, null, NOW);   // hoàn nhiều lần

        assertThat(b.refundDue()).isZero();
        assertThat(b.getPaidAmount()).isEqualTo(1_000_000);                         // phần phí phạt được giữ lại
        assertThat(b.getRefunds()).hasSize(2);
    }

    @Test
    void moneyHistoryMixesPaymentsAndRefundsAndAddsUpToPaidAmount() {
        Booking b = paidBooking(5, adultPassenger("A"), adultPassenger("B"));
        b.approveCancellation(b.requestCancellation(CancellationScope.FULL, null, null, null, NOW), null, null, NOW);
        b.refund(600_000, PaymentMethod.BANK_TRANSFER, "HT1", null, null, null, null, NOW.plusHours(1));
        b.refund(400_000, PaymentMethod.CASH, null, null, null, null, null, NOW.plusHours(2));

        List<MoneyTransaction> history = b.moneyHistory();

        assertThat(history).extracting(MoneyTransaction::direction)
                .containsExactly(MoneyDirection.IN, MoneyDirection.OUT, MoneyDirection.OUT);
        assertThat(history).extracting(MoneyTransaction::signedAmount).containsExactly(2_000_000L, -600_000L, -400_000L);
        assertThat(history.stream().mapToLong(MoneyTransaction::signedAmount).sum()).isEqualTo(b.getPaidAmount());
    }

    @Test
    void noPolicyMeansNoPenalty() {
        Booking b = paidBooking(1, adultPassenger("A"), adultPassenger("B"));
        ReflectionTestUtils.setField(b, "cancellationPolicy", null);

        assertThat(b.penaltyPercentAt(NOW.toLocalDate())).isZero();
    }

    @Test
    void transferToPricierDepartureMakesPaidBookingDeposited() {
        Booking b = paidBooking(10, adultPassenger("A"), adultPassenger("B"));
        Tour tour = b.getDeparture().getTour();
        ReflectionTestUtils.setField(tour, "id", 1L);
        ReflectionTestUtils.setField(b.getDeparture(), "id", 10L);
        Departure later = new Departure(tour, NOW.plusDays(20), NOW.plusDays(20).plusHours(9), NOW.plusDays(19), 20, NOW);
        ReflectionTestUtils.setField(later, "id", 11L);

        DepartureTransfer t = b.transferTo(later, List.of(
                        new Passenger.Details("A", LocalDate.of(1990, 1, 1), null, null, null, adult, 1_200_000, null, null, 0),
                        new Passenger.Details("B", LocalDate.of(1990, 1, 1), null, null, null, adult, 1_200_000, null, null, 0)),
                List.of(), null, "Khách xin đổi ngày", NOW);

        assertThat(t.getPriceDifference()).isEqualTo(400_000);
        assertThat(b.getDeparture()).isSameAs(later);
        assertThat(b.getStatus()).isEqualTo(BookingStatus.DEPOSITED);
        assertThat(b.remainingAmount()).isEqualTo(400_000);
    }

    private static <T> T withId(T entity, Long id) {
        ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }

    private static void assertCode(org.assertj.core.api.ThrowableAssert.ThrowingCallable call, String code) {
        assertThatThrownBy(call).isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getCode()).isEqualTo(code));
    }
}
