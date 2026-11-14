package vn.edu.tvu.vinhlongtour.booking.service;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import vn.edu.tvu.vinhlongtour.booking.web.BookingDtos.PassengerInput;
import vn.edu.tvu.vinhlongtour.booking.web.BookingDtos.SurchargeSelection;
import vn.edu.tvu.vinhlongtour.catalog.domain.AgeRange;
import vn.edu.tvu.vinhlongtour.catalog.domain.PassengerType;
import vn.edu.tvu.vinhlongtour.catalog.domain.PickupPoint;
import vn.edu.tvu.vinhlongtour.catalog.domain.Surcharge;
import vn.edu.tvu.vinhlongtour.catalog.domain.SurchargeMethod;
import vn.edu.tvu.vinhlongtour.common.ApiException;
import vn.edu.tvu.vinhlongtour.tour.domain.Departure;
import vn.edu.tvu.vinhlongtour.tour.domain.DeparturePickup;
import vn.edu.tvu.vinhlongtour.tour.domain.DeparturePrice;
import vn.edu.tvu.vinhlongtour.tour.domain.Itinerary;
import vn.edu.tvu.vinhlongtour.tour.domain.Tour;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Kiểm tra cách tính giá đơn: tuổi theo ngày khởi hành, loại khách, người lớn bắt buộc, phí đón,
 * phụ thu và tiền cọc. Dựng dữ liệu trong bộ nhớ, không cần CSDL.
 */
class PriceCalculatorTest {

    private static final LocalDateTime DEPARTURE = LocalDateTime.of(2026, 12, 1, 7, 0);

    private final PassengerType adult = withId(new PassengerType("Người lớn", new AgeRange(12, 120), 1), 1L);
    private final PassengerType child = withId(new PassengerType("Trẻ em", new AgeRange(5, 11), 1), 2L);
    private final PassengerType infant = withId(new PassengerType("Em bé", new AgeRange(0, 4), 0), 3L);
    private final PickupPoint busStation = withId(new PickupPoint("Bến xe Vĩnh Long", null), 10L);
    private final Surcharge insurance = withId(new Surcharge("Bảo hiểm", SurchargeMethod.PER_PERSON, 20_000, null), 20L);
    private final Surcharge singleRoom = withId(new Surcharge("Phòng đơn", SurchargeMethod.PER_UNIT, 300_000, null), 21L);
    private final Surcharge notInTour = withId(new Surcharge("Thuê xe đạp", SurchargeMethod.PER_UNIT, 50_000, null), 22L);
    private final Departure departure = departure();
    private final PriceCalculator calculator = new PriceCalculator(30);

    private Departure departure() {
        Tour tour = new Tour("VL1N", "Vĩnh Long 1 ngày", 1, 0);
        tour.replaceItinerary(List.of(new Itinerary.DayPlan("Ngày 1", null, List.of())));
        tour.setSurcharges(Set.of(insurance, singleRoom));
        Departure d = new Departure(tour, DEPARTURE, DEPARTURE.plusHours(10), DEPARTURE.minusDays(1), 20, DEPARTURE.minusDays(30));
        d.setPrices(List.of(new DeparturePrice(adult, 900_000), new DeparturePrice(child, 450_000), new DeparturePrice(infant, 0)),
                List.of(adult, child, infant));
        d.setPickups(List.of(new DeparturePickup(busStation, LocalTime.of(6, 30), 50_000)));
        return d;
    }

    private static PassengerInput born(LocalDate birthDate) {
        return new PassengerInput("Khách " + birthDate, birthDate, null, null, null, null);
    }

    @Test
    void ageIsCountedOnDepartureDayNotToday() {
        var q = calculator.quote(departure, List.of(
                born(LocalDate.of(1990, 5, 5)),
                born(LocalDate.of(2014, 12, 1)),     // tròn 12 tuổi đúng ngày đi: người lớn
                born(LocalDate.of(2014, 12, 2))),    // còn 11 tuổi ngày đi: trẻ em
                null, List.of(adult, child, infant));

        assertThat(q.passengers()).extracting(l -> l.details().passengerType().getName())
                .containsExactly("Người lớn", "Người lớn", "Trẻ em");
        assertThat(q.ticketTotal()).isEqualTo(900_000 + 900_000 + 450_000);
    }

    @Test
    void bookingNeedsSomeoneAged18OrOlder() {
        assertCode(() -> calculator.quote(departure, List.of(born(LocalDate.of(2010, 1, 1)), born(LocalDate.of(2018, 1, 1))),
                null, List.of(adult, child, infant)), "ADULT_REQUIRED");
    }

    @Test
    void infantTakesNoSeatAndPickupFeeIsPerPassenger() {
        PassengerInput parent = new PassengerInput("Bố", LocalDate.of(1990, 1, 1), null, null, null, 10L);
        PassengerInput baby = new PassengerInput("Bé", LocalDate.of(2025, 1, 1), null, null, null, 10L);

        var q = calculator.quote(departure, List.of(parent, baby), null, List.of(adult, child, infant));

        assertThat(q.seats()).isEqualTo(1);
        assertThat(q.pickupTotal()).isEqualTo(100_000);
        assertThat(q.total()).isEqualTo(900_000 + 0 + 100_000);
    }

    @Test
    void pickupPointMustBelongToDeparture() {
        PassengerInput p = new PassengerInput("A", LocalDate.of(1990, 1, 1), null, null, null, 999L);
        assertCode(() -> calculator.quote(departure, List.of(p), null, List.of(adult, child, infant)), "INVALID_PICKUP");
    }

    @Test
    void surchargesAreCountedByTheirMethodAndMustBelongToTour() {
        List<PassengerInput> two = List.of(born(LocalDate.of(1990, 1, 1)), born(LocalDate.of(1992, 1, 1)));

        var q = calculator.quote(departure, two,
                List.of(new SurchargeSelection(20L, null), new SurchargeSelection(21L, 1)), List.of(adult, child, infant));

        assertThat(q.surchargeTotal()).isEqualTo(2 * 20_000 + 300_000);
        assertCode(() -> calculator.quote(departure, two, List.of(new SurchargeSelection(22L, 1)), List.of(adult, child, infant)),
                "SURCHARGE_NOT_AVAILABLE");
        assertCode(() -> calculator.quote(departure, two, List.of(new SurchargeSelection(20L, null), new SurchargeSelection(20L, null)),
                List.of(adult, child, infant)), "DUPLICATE_SURCHARGE");
    }

    @Test
    void depositIsRoundedUpToThousandsAndNeverAboveTotal() {
        assertThat(calculator.deposit(1_234_567)).isEqualTo(371_000);   // 30% = 370.370,1 -> 371.000
        assertThat(calculator.deposit(900_000)).isEqualTo(270_000);
        assertThat(calculator.deposit(500)).isEqualTo(500);
        assertThat(calculator.deposit(0)).isZero();
    }

    private static <T> T withId(T entity, Long id) {
        ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }

    private static void assertCode(org.assertj.core.api.ThrowableAssert.ThrowingCallable call, String code) {
        assertThatThrownBy(call).isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getCode()).isEqualTo(code));
    }
}
